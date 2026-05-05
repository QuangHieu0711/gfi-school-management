import os
import re
import unicodedata
from typing import Optional
import logging

import torch
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer

# Suppress HF Hub warnings
os.environ['HF_HUB_DISABLE_TELEMETRY'] = '1'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

logger = logging.getLogger(__name__)


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
    "Hãy viết đúng 1 câu nhận xét học sinh ngắn gọn, tự nhiên, lịch sự. "
    "Câu trả lời phải bắt đầu bằng từ Em. "
    "Tuyệt đối chỉ dùng tiếng Việt có dấu. "
    "Không dùng tiếng Anh, tiếng Trung, tiếng Indonesia hoặc bất kỳ ngôn ngữ nước ngoài nào. "
    "Không nhắc tên học sinh, không viết tiêu đề, không lặp lại đề bài."
)

# Những từ/cụm từ nước ngoài mô hình hay tự chèn.
BAD_WORDS = [
    "kepada", "direction", "clearer", "assignedhomework", "assigned homework",
    "homework", "point score", "point", "score", "practice", "exercise",
    "complete", "completed", "activity", "movement", "student", "teacher",
    "lesson", "skill", "skills", "performance", "teamwork", "good", "better",
    "run", "jump", "throw", "catch", "ball", "the", "and", "for", "with",
    "has", "have", "had", "not", "but", "can", "should", "would", "could",
    "more", "very", "well", "also", "need", "needs", "improve", "improvement",
    "excellent", "try", "work", "hard", "class", "test", "exam", "grade",
    "result", "results", "focus", "attention", "confident", "confidence",
    "participate", "participation", "behavior", "behaviour", "effort",
    "progress", "learning", "study", "training", "develop", "development",
    "achieve", "achievement", "understand", "understanding", "knowledge",
    "ability", "capable", "correct", "incorrect", "basic", "advanced",
    "level", "task", "group", "individual", "cooperation", "attitude",
    "positive", "negative", "strong", "weak", "fast", "slow", "high", "low",
    "overall", "general", "specific", "important", "necessary",
    "bola", "pointscore", "point score",
    "回答", "练习", "预料",
]

REPLACEMENTS = {
    "kepada": "với",
    "tình yêu với": "tình yêu thương với",
    "direction": "hướng",
    "clearer": "rõ ràng hơn",
    "assignedhomework": "bài tập",
    "assigned homework": "bài tập",
    "point score": "phối hợp",
    "pointscore": "phối hợp",
    "bola": "bóng",
    "homework": "bài tập",
    "exercise": "bài tập",
    "practice": "luyện tập",
    "skill": "kĩ năng",
    "skills": "kĩ năng",
    "activity": "hoạt động",
    "movement": "vận động",
    "student": "học sinh",
    "teacher": "giáo viên",
    "lesson": "bài học",
    "performance": "thể hiện",
    "teamwork": "phối hợp nhóm",
    "complete": "hoàn thành",
    "completed": "hoàn thành",
    "good": "tốt",
    "better": "tốt hơn",
    "improve": "cải thiện",
    "effort": "nỗ lực",
    "progress": "tiến bộ",
    "confident": "tự tin",
    "confidence": "sự tự tin",
    "focus": "tập trung",
    "attention": "chú ý",
    "excellent": "xuất sắc",
    "participate": "tham gia",
    "participation": "sự tham gia",
    "learning": "học tập",
    "training": "rèn luyện",
    "understand": "hiểu",
    "understanding": "sự hiểu biết",
    "correct": "chính xác",
    "basic": "cơ bản",
    "cooperation": "hợp tác",
    "attitude": "thái độ",
    "positive": "tích cực",
    "strong": "mạnh",
    "weak": "yếu",
    "fast": "nhanh",
    "slow": "chậm",
    "回答": "trả lời",
    "练习": "luyện tập",
    "预料": "dự đoán",
}

from viet_syllables_set import VIET_SYLLABLES

# ── Phát hiện từ nước ngoài bằng WHITELIST tiếng Việt ──────────────────────
# Bất kỳ từ nào chỉ gồm ký tự ASCII a-z mà KHÔNG nằm trong whitelist
# sẽ bị coi là từ nước ngoài. Đây là cách triệt để nhất.
_VIET_ASCII_WHITELIST = VIET_SYLLABLES | {
    "ok", "km", "kg", "cm", "mm",
}


def _strip_viet_diacritics(text: str) -> str:
    """Remove Vietnamese diacritics, returning ASCII-only lowercase."""
    nfkd = unicodedata.normalize("NFD", text.lower())
    # Bỏ combining marks
    base = "".join(c for c in nfkd if unicodedata.category(c) != "Mn")
    # Xử lý đ -> d
    base = base.replace("đ", "d")
    return base


def _is_pure_ascii_word(word: str) -> bool:
    """Check if a word contains only ASCII a-z letters."""
    return bool(re.fullmatch(r"[a-zA-Z]+", word))


def _contains_foreign_words(text: str) -> bool:
    """Detect foreign words using whitelist approach.
    Any word that is pure ASCII a-z and NOT in the Vietnamese whitelist
    is considered foreign.
    """
    for word in re.findall(r"[a-zA-ZÀ-ỹĐđ]+", text):
        ascii_form = _strip_viet_diacritics(word)
        # Nếu sau khi bỏ dấu mà chỉ toàn ASCII → kiểm tra whitelist
        if _is_pure_ascii_word(ascii_form):
            if ascii_form not in _VIET_ASCII_WHITELIST and len(ascii_form) >= 2:
                return True
    return False


def _remove_foreign_words(text: str) -> str:
    """Remove any foreign word from the text."""
    def _replace_word(m: re.Match) -> str:
        word = m.group(0)
        ascii_form = _strip_viet_diacritics(word)
        if _is_pure_ascii_word(ascii_form) and ascii_form not in _VIET_ASCII_WHITELIST and len(ascii_form) >= 2:
            return ""
        return word

    result = re.sub(r"[a-zA-ZÀ-ỹĐđ]+", _replace_word, text)
    # Dọn dẹp khoảng trắng thừa
    result = re.sub(r"\s+", " ", result).strip()
    return result


# CJK + known foreign words regex (giữ lại để check nhanh)
FOREIGN_TEXT_RE = re.compile(
    r"[\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]",
    re.IGNORECASE,
)

DEFAULT_COMMENT = "Em cần tiếp tục cố gắng trong học tập."

tokenizer = None
model = None
bad_words_ids = None
_model_load_failed = False  # Track if we already tried and failed to load


def get_model_device():
    return next(model.parameters()).device


def build_bad_words_ids():
    """Build token ids that should be blocked during generation."""
    ids = []
    seen = set()

    for word in BAD_WORDS:
        forms = {
            word,
            word.lower(),
            word.capitalize(),
            word.upper(),
            " " + word,
            " " + word.capitalize(),
            "\n" + word,
        }
        for form in forms:
            token_ids = tokenizer.encode(form, add_special_tokens=False)
            key = tuple(token_ids)
            if token_ids and key not in seen:
                ids.append(token_ids)
                seen.add(key)

    return ids


def load_model(device: str = "cpu"):
    """Load tokenizer, base model, and LoRA adapter once at service startup."""
    global tokenizer, model, bad_words_ids, _model_load_failed

    # If we already know loading fails, don't retry
    if _model_load_failed:
        logger.warning("Model loading previously failed; skipping retry.")
        return

    if model is not None and tokenizer is not None:
        return

    adapter_path = os.environ.get("ADAPTER_PATH", ADAPTER_PATH)
    if not os.path.isdir(adapter_path):
        _model_load_failed = True
        raise FileNotFoundError(f"Adapter path does not exist: {adapter_path}")

    try:
        # Pre-check: try loading in subprocess first to detect early failures
        from model_loader_subprocess import load_model_in_subprocess
        if not load_model_in_subprocess(BASE_MODEL, adapter_path, timeout_sec=120):
            logger.error("Model loading failed in subprocess; using fallback comments.")
            _model_load_failed = True
            return

        # Now load in-process if subprocess check passed
        tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL, trust_remote_code=True)
        if tokenizer.pad_token_id is None:
            tokenizer.pad_token = tokenizer.eos_token

        kwargs = {"trust_remote_code": True}
        if device == "cuda":
            kwargs.update({"device_map": "auto", "torch_dtype": torch.float16})
        else:
            kwargs.update({"low_cpu_mem_usage": False})  # Changed from True

        base_model = AutoModelForCausalLM.from_pretrained(BASE_MODEL, **kwargs)
        model = PeftModel.from_pretrained(base_model, adapter_path)

        if device == "cpu":
            model.to("cpu")

        model.eval()
        bad_words_ids = build_bad_words_ids()
        
        logger.info("Model loaded successfully.")
        
    except Exception as e:
        logger.exception(f"Failed to load model: {e}")
        _model_load_failed = True
        # Don't re-raise; let the caller use fallback


def build_prompt(data: dict, extra_instruction: str = "") -> str:
    """Create a Vietnamese-only prompt from backend request data."""
    attendance_full = "Có" if data.get("attendance_full") else "Không"
    extra_line = f"\n- {extra_instruction}" if extra_instruction else ""

    lesson_title = str(data.get("lesson_title", ""))
    lesson_title = re.sub(r'(?i)(bài|tiết)\s*\d+[\s\-:]*', '', lesson_title).strip()
    
    objective = str(data.get("learning_objective", ""))
    objective = re.sub(r'(?i)(bài|tiết)\s*\d+[\s\-:]*', '', objective).strip()

    return f"""Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: {data.get("grade_level", "")}
Môn học: {data.get("subject_name", "")}
Học kì: {data.get("term", "")}
Tuần: {data.get("week_no", "")}
Tiết: {data.get("lesson_no", "")}
Tên bài học: {lesson_title}
Mục tiêu bài học: {objective}
Mức đánh giá: {data.get("evaluation", "")}
Đi học đầy đủ: {attendance_full}
Mức độ tham gia: {data.get("participation_level", "")}
Thái độ: {data.get("behavior_tag", "")}
Bộ sách: {data.get("textbook_series", "")}

Yêu cầu bắt buộc:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt có dấu.
- Không dùng tiếng Anh hoặc từ nước ngoài.
- Không nhắc tên học sinh.
- Tuyệt đối không lặp lại chữ 'Bài' hay 'Tiết' trong nhận xét.
- Không giải thích.{extra_line}"""


def has_foreign_text(text: str) -> bool:
    """Return True if the sentence contains any foreign-language fragments.
    Uses both CJK regex AND the Vietnamese whitelist approach.
    """
    if not text:
        return False
    if FOREIGN_TEXT_RE.search(text):
        return True
    return _contains_foreign_words(text)


def clean_response(text: str) -> Optional[str]:
    """Normalize common bad tokens and keep only one short Vietnamese sentence."""
    if not text:
        return None

    text = text.strip().strip('"""\'\'`')
    text = re.sub(
        r"^(?:assistant|nhận xét|comment|answer|trả lời)\s*[:：\-]\s*",
        "",
        text,
        flags=re.IGNORECASE,
    )

    for bad, good in REPLACEMENTS.items():
        text = re.sub(re.escape(bad), good, text, flags=re.IGNORECASE)

    if has_foreign_text(text):
        return None

    match = re.match(r"(.+?[.!?])(\s|$)", text)
    if match:
        text = match.group(1).strip()

    if not text:
        return None

    # Dọn dẹp khoảng trắng thừa và chuẩn hóa dấu câu
    text = re.sub(r"\s+([,.!?;:])", r"\1", text)
    text = re.sub(r"([,.!?;:])(?=[^\s])", r"\1 ", text)
    text = re.sub(r"\s+", " ", text).strip()

    # Xử lý viết hoa/viết thường: viết thường tất cả trước, sau đó viết hoa đầu câu
    text = text.lower()

    def capitalize_sentence(match):
        return match.group(1) + match.group(2).upper()

    text = re.sub(r'(^|[.!?]\s+)([a-zà-ỹđ])', capitalize_sentence, text)

    if text.startswith("em "):
        text = "Em " + text[3:]
    elif not text.startswith("Em"):
        text = "Em " + text[0].lower() + text[1:] if text else ""

    if text and text[-1] not in ".!?":
        text += "."

    # Từ chối nếu câu có dấu gạch ngang lơ lửng hoặc giới từ đứng trước dấu câu
    if re.search(r"-\s*[,.!?;:]", text) or re.search(r"\b(với|của|và|hoặc|nhưng|để|cho|bằng)\b\s*[,.!?;:]", text, flags=re.IGNORECASE):
        return None

    # Câu quá ngắn → không hợp lệ.
    word_count = len(re.findall(r"[a-zA-ZÀ-ỹĐđ]+", text))
    if word_count < 4:
        return None

    return text


def _safe_lesson_phrase(data: dict) -> str:
    """Create a safe Vietnamese phrase for fallback comments."""
    raw = str(data.get("lesson_title") or data.get("learning_objective") or "").strip()
    raw = re.sub(r'(?i)(bài|tiết)\s*\d+[\s\-:]*', '', raw)
    
    for bad, good in REPLACEMENTS.items():
        raw = re.sub(re.escape(bad), good, raw, flags=re.IGNORECASE)

    raw = _remove_foreign_words(raw)
    raw = FOREIGN_TEXT_RE.sub("", raw)
    raw = re.sub(r"[^0-9A-Za-zÀ-ỹĐđ\s,;:()./\-]", "", raw)
    raw = re.sub(r"\s+", " ", raw).strip(" .,:;-")

    if not raw:
        return "nội dung bài học"

    if len(raw) > 80:
        raw = raw[:80].rsplit(" ", 1)[0].strip(" .,:;-")

    return raw[0].lower() + raw[1:]


def fallback_comment(data: dict) -> str:
    """Always return a safe Vietnamese-only comment if model output is invalid."""
    lesson = _safe_lesson_phrase(data)
    evaluation = str(data.get("evaluation", "")).strip().upper()

    if evaluation.startswith("T"):
        return f"Em thực hiện tốt {lesson} và biết phối hợp với bạn khi luyện tập."
    if evaluation.startswith("H"):
        return f"Em hoàn thành {lesson} theo hướng dẫn, cần rèn thêm để động tác chính xác hơn."
    if evaluation.startswith("C"):
        return f"Em cần cố gắng hơn khi thực hiện {lesson} và chú ý luyện tập theo hướng dẫn."

    return DEFAULT_COMMENT


def _generate_raw(prompt: str, max_new_tokens: int, do_sample: bool, temperature: float, top_p: float) -> str:
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

    generation_kwargs = {
        **encoded,
        "max_new_tokens": max_new_tokens,
        "do_sample": do_sample,
        "repetition_penalty": 1.2,
        "no_repeat_ngram_size": 4,
        "bad_words_ids": bad_words_ids or None,
        "eos_token_id": tokenizer.eos_token_id,
        "pad_token_id": tokenizer.eos_token_id,
    }
    if do_sample:
        generation_kwargs.update({"temperature": temperature, "top_p": top_p})

    with torch.no_grad():
        output = model.generate(**generation_kwargs)

    return tokenizer.decode(
        output[0][input_ids.shape[-1] :],
        skip_special_tokens=True,
    ).strip()


def generate_comment(data: dict, max_new_tokens: int = 40) -> str:
    """Generate one cleaned Vietnamese student comment for the backend."""
    # Lazily load the model on first request. Wrap in try/except so
    # the API can return a safe fallback if model loading fails.
    global model, tokenizer, _model_load_failed
    
    if _model_load_failed:
        # We already know loading failed; return fallback immediately
        logger.debug("Model is unavailable; returning fallback comment.")
        return fallback_comment(data)
    
    if model is None or tokenizer is None:
        try:
            load_model()
        except Exception as e:
            logger.exception(f"Model failed to load during request: {e}")
            _model_load_failed = True
            return fallback_comment(data)
    
    # If still no model after load attempt, use fallback
    if model is None or tokenizer is None:
        logger.debug("Model not loaded; using fallback comment.")
        return fallback_comment(data)

    attempts = [
        {
            "do_sample": False,
            "temperature": 0.0,
            "top_p": 1.0,
            "extra_instruction": "",
        },
        {
            "do_sample": True,
            "temperature": 0.25,
            "top_p": 0.75,
            "extra_instruction": (
                "Nếu câu có lẫn tiếng Anh hoặc từ nước ngoài, hãy viết lại hoàn toàn bằng tiếng Việt."
            ),
        },
    ]

    for cfg in attempts:
        prompt = build_prompt(data, extra_instruction=cfg["extra_instruction"])
        raw = _generate_raw(
            prompt=prompt,
            max_new_tokens=max_new_tokens,
            do_sample=cfg["do_sample"],
            temperature=cfg["temperature"],
            top_p=cfg["top_p"],
        )
        cleaned = clean_response(raw)
        if cleaned:
            return cleaned

    return fallback_comment(data)
