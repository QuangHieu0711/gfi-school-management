import os
import re

import torch
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer


BASE_MODEL = "Qwen/Qwen2.5-1.5B-Instruct"
ADAPTER_PATH = os.path.join(
    os.path.dirname(__file__),
    "gfi_comments_lora_final",
    "saves",
    "qwen2_5_1_5b",
    "gfi_comments_lora",
    "checkpoint-2469",
)

SYSTEM_PROMPT = (
    "Bạn là giáo viên tiểu học Việt Nam. "
    "Hãy viết một câu nhận xét học sinh ngắn gọn, tự nhiên, lịch sự. "
    "Chỉ dùng tiếng Việt. "
    "Câu trả lời phải bắt đầu bằng từ Em. "
    "Không nhắc tên học sinh, không viết tiêu đề, không lặp lại đề bài."
)

BAD_WORDS = [
    "kepada",
    "direction",
    "clearer",
    "assignedhomework",
    "回答",
    "练习",
    "预料",
]

REPLACEMENTS = {
    "kepada": "với",
    "tình yêu với": "tình yêu thương với",
    "direction": "hướng",
    "clearer": "rõ ràng hơn",
    "assignedhomework": "bài tập",
    "回答": "trả lời",
    "练习": "luyện tập",
    "预料": "dự đoán",
}

DEFAULT_COMMENT = "Em cần tiếp tục cố gắng trong học tập."

tokenizer = None
model = None
bad_words_ids = None


def get_model_device():
    return next(model.parameters()).device


def build_bad_words_ids():
    """Build token ids that should be blocked during generation."""
    ids = []
    for word in BAD_WORDS:
        token_ids = tokenizer.encode(word, add_special_tokens=False)
        if token_ids:
            ids.append(token_ids)
    return ids


def load_model(device: str = "cpu"):
    """Load tokenizer, base model, and LoRA adapter once at service startup."""
    global tokenizer, model, bad_words_ids

    if model is not None and tokenizer is not None:
        return

    adapter_path = os.environ.get("ADAPTER_PATH", ADAPTER_PATH)
    if not os.path.isdir(adapter_path):
        raise FileNotFoundError(f"Adapter path does not exist: {adapter_path}")

    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL, trust_remote_code=True)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token

    kwargs = {"trust_remote_code": True}
    if device == "cuda":
        kwargs.update({"device_map": "auto", "torch_dtype": torch.float16})
    else:
        kwargs.update({"low_cpu_mem_usage": True})

    base_model = AutoModelForCausalLM.from_pretrained(BASE_MODEL, **kwargs)
    model = PeftModel.from_pretrained(base_model, adapter_path)

    if device == "cpu":
        model.to("cpu")

    model.eval()
    bad_words_ids = build_bad_words_ids()


def build_prompt(data: dict) -> str:
    """Create a Vietnamese-only prompt from backend request data."""
    attendance_full = "Có" if data.get("attendance_full") else "Không"

    return f"""Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: {data.get("grade_level", "")}
Môn học: {data.get("subject_name", "")}
Học kì: {data.get("term", "")}
Tuần: {data.get("week_no", "")}
Tiết: {data.get("lesson_no", "")}
Tên bài học: {data.get("lesson_title", "")}
Mục tiêu bài học: {data.get("learning_objective", "")}
Mức đánh giá: {data.get("evaluation", "")}
Đi học đầy đủ: {attendance_full}
Mức độ tham gia: {data.get("participation_level", "")}
Thái độ: {data.get("behavior_tag", "")}
Bộ sách: {data.get("textbook_series", "")}

Yêu cầu:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt.
- Không nhắc tên học sinh.
- Không giải thích."""


def clean_response(text: str) -> str:
    """Normalize common bad tokens and keep only one short sentence."""
    for bad, good in REPLACEMENTS.items():
        text = text.replace(bad, good)

    text = text.strip()
    match = re.match(r"(.+?[.!?])(\s|$)", text)
    if match:
        text = match.group(1).strip()

    if not text:
        return DEFAULT_COMMENT

    if not text.startswith("Em"):
        text = "Em " + text[0].lower() + text[1:]

    return text


def generate_comment(data: dict, max_new_tokens: int = 45) -> str:
    """Generate one cleaned Vietnamese student comment for the backend."""
    load_model()

    prompt = build_prompt(data)
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": prompt},
    ]

    chat_text = tokenizer.apply_chat_template(
        messages,
        add_generation_prompt=True,
        tokenize=False,
    )
    encoded = tokenizer(chat_text, return_tensors="pt")
    encoded = {key: value.to(get_model_device()) for key, value in encoded.items()}
    input_ids = encoded["input_ids"]

    with torch.no_grad():
        output = model.generate(
            **encoded,
            max_new_tokens=max_new_tokens,
            do_sample=False,
            repetition_penalty=1.25,
            no_repeat_ngram_size=4,
            bad_words_ids=bad_words_ids,
            eos_token_id=tokenizer.eos_token_id,
            pad_token_id=tokenizer.eos_token_id,
        )

    response = tokenizer.decode(
        output[0][input_ids.shape[-1] :],
        skip_special_tokens=True,
    ).strip()

    return clean_response(response)
