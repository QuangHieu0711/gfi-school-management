#!/usr/bin/env python3
"""
Compare the base Qwen model and a LoRA adapter on Vietnamese report-comment prompts.

Example:
  python test_lora_inference.py --device cpu

The script writes:
  outputs/base_outputs.jsonl
  outputs/lora_outputs.jsonl
"""

import argparse
import gc
import json
import os
import re
import sys


DEFAULT_BASE_MODEL = "Qwen/Qwen2.5-1.5B-Instruct"
DEFAULT_ADAPTER_PATH = (
    "gfi_comments_lora_final/saves/qwen2_5_1_5b/gfi_comments_lora/checkpoint-2469"
)


PROMPTS = [
    """Viết nhận xét cho học sinh dựa trên thông tin sau:

Lớp: 3A
Khối: Lớp 3
Môn học: Tự nhiên và Xã hội
Học kì: GK1
Tuần: 1
Tiết: 1
Tên bài học: Gia đình em
Mục tiêu bài học: Giới thiệu được bản thân và các thành viên trong gia đình. Biết thể hiện tình cảm với người thân.
Mức đánh giá: Tốt
Đi học đầy đủ: Có
Mức độ tham gia: Đều
Thái độ: Yêu thương
Bộ sách: KẾT NỐI TRI THỨC VỚI CUỘC SỐNG

Yêu cầu:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt.
- Không nhắc tên học sinh.
- Không giải thích."""
]


SYSTEM_PROMPT = (
    "Bạn là giáo viên tiểu học Việt Nam. "
    "Hãy viết một câu nhận xét học sinh ngắn gọn, tự nhiên, lịch sự. "
    "Chỉ dùng tiếng Việt. "
    "Không dùng tiếng Anh, tiếng Trung, tiếng Indonesia hoặc ngôn ngữ khác. "
    "Câu trả lời phải bắt đầu bằng từ Em. "
    "Không nhắc tên học sinh, không viết tiêu đề, không lặp lại đề bài."
)


BANNED_FOREIGN_WORDS = [
    "kepada",
    "clearer",
    "direction",
    "student",
    "teacher",
    "family",
    "lesson",
    "good",
    "great",
    "excellent",
    "need",
    "needs",
    "with",
    "and",
    "the",
    "for",
    "to",
]

RETRY_PROMPT_SUFFIX = (
    "\n\nLưu ý bắt buộc: Câu trả lời chỉ được dùng tiếng Việt. "
    "Nếu định viết từ nước ngoài, hãy thay bằng từ tiếng Việt phù hợp."
)


def check_imports():
    missing = []
    for package in ("torch", "transformers", "peft", "accelerate"):
        try:
            __import__(package)
        except Exception:
            missing.append(package)

    if missing:
        print("Missing required packages:", ", ".join(missing))
        print("Install them in your venv, for example:")
        print("  pip install torch transformers accelerate peft")
        sys.exit(2)


def model_device(model):
    return next(model.parameters()).device


def build_bad_words_ids(tokenizer):
    bad_words_ids = []
    for word in BANNED_FOREIGN_WORDS:
        variants = {word, word.capitalize(), " " + word, " " + word.capitalize()}
        for variant in variants:
            token_ids = tokenizer(
                variant,
                add_special_tokens=False,
            ).input_ids
            if token_ids:
                bad_words_ids.append(token_ids)
    return bad_words_ids


def find_foreign_flags(text):
    flags = []
    lowered = text.lower()

    for word in BANNED_FOREIGN_WORDS:
        if re.search(rf"\b{re.escape(word.lower())}\b", lowered):
            flags.append(word)

    if re.search(r"[\u4e00-\u9fff]", text):
        flags.append("chinese_character")

    return sorted(set(flags))


def load_tokenizer_and_model(model_name, device, low_cpu_mem_usage=True):
    import torch
    from transformers import AutoModelForCausalLM, AutoTokenizer

    tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token

    kwargs = {"trust_remote_code": True}
    if device == "cuda":
        kwargs.update({"device_map": "auto", "torch_dtype": torch.float16})
    elif low_cpu_mem_usage:
        kwargs.update({"low_cpu_mem_usage": True})

    model = AutoModelForCausalLM.from_pretrained(model_name, **kwargs)
    if device == "cpu":
        model.to("cpu")
    model.eval()

    return tokenizer, model


def generate_once(tokenizer, model, prompt, device, bad_words_ids, max_new_tokens):
    import torch

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": prompt},
    ]

    chat_text = tokenizer.apply_chat_template(
        messages,
        add_generation_prompt=True,
        tokenize=False,
    )
    encoded = tokenizer(chat_text, return_tensors="pt").to(device)
    input_ids = encoded["input_ids"]

    with torch.no_grad():
        output = model.generate(
            **encoded,
            max_new_tokens=max_new_tokens,
            do_sample=False,
            repetition_penalty=1.25,
            no_repeat_ngram_size=4,
            eos_token_id=tokenizer.eos_token_id,
            pad_token_id=tokenizer.eos_token_id,
            bad_words_ids=bad_words_ids,
        )

    return tokenizer.decode(
        output[0][input_ids.shape[-1] :],
        skip_special_tokens=True,
    ).strip()


def generate_for_prompts(tokenizer, model, prompts, max_new_tokens=45, max_retries=2):
    results = []
    device = model_device(model)
    bad_words_ids = build_bad_words_ids(tokenizer)

    for prompt in prompts:
        response = ""
        foreign_flags = []
        attempts = 0

        for attempt in range(max_retries + 1):
            attempts = attempt + 1
            retry_suffix = RETRY_PROMPT_SUFFIX * attempt
            response = generate_once(
                tokenizer,
                model,
                prompt + retry_suffix,
                device,
                bad_words_ids,
                max_new_tokens,
            )
            foreign_flags = find_foreign_flags(response)
            if not foreign_flags:
                break

        results.append(
            {
                "prompt": prompt,
                "response": response,
                "valid": not foreign_flags,
                "foreign_flags": foreign_flags,
                "attempts": attempts,
            }
        )

    return results


def write_jsonl(path, rows):
    with open(path, "w", encoding="utf-8") as file:
        for row in rows:
            file.write(json.dumps(row, ensure_ascii=False) + "\n")


def clear_memory():
    gc.collect()
    try:
        import torch

        if torch.cuda.is_available():
            torch.cuda.empty_cache()
    except Exception:
        pass


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-model", default=DEFAULT_BASE_MODEL)
    parser.add_argument("--adapter-path", default=DEFAULT_ADAPTER_PATH)
    parser.add_argument("--device", choices=["cpu", "cuda"], default="cpu")
    parser.add_argument("--out-dir", default="outputs")
    parser.add_argument("--max-new-tokens", type=int, default=45)
    parser.add_argument("--max-retries", type=int, default=2)
    return parser.parse_args()


def main():
    check_imports()
    args = parse_args()

    adapter_path = os.path.abspath(args.adapter_path)
    if not os.path.isdir(adapter_path):
        print("Adapter path does not exist:", adapter_path)
        sys.exit(3)

    os.makedirs(args.out_dir, exist_ok=True)

    print("Loading base model...")
    tokenizer, base_model = load_tokenizer_and_model(args.base_model, args.device)

    print("Generating with base model...")
    base_results = generate_for_prompts(
        tokenizer,
        base_model,
        PROMPTS,
        max_new_tokens=args.max_new_tokens,
        max_retries=args.max_retries,
    )
    base_out_path = os.path.join(args.out_dir, "base_outputs.jsonl")
    write_jsonl(base_out_path, base_results)

    del base_model
    clear_memory()

    print("Loading base model and attaching LoRA adapter...")
    tokenizer, model_for_lora = load_tokenizer_and_model(args.base_model, args.device)

    print("Applying LoRA adapter from:", adapter_path)
    from peft import PeftModel

    model_lora = PeftModel.from_pretrained(model_for_lora, adapter_path)
    if args.device == "cpu":
        model_lora.to("cpu")
    model_lora.eval()

    print("Generating with LoRA model...")
    lora_results = generate_for_prompts(
        tokenizer,
        model_lora,
        PROMPTS,
        max_new_tokens=args.max_new_tokens,
        max_retries=args.max_retries,
    )
    lora_out_path = os.path.join(args.out_dir, "lora_outputs.jsonl")
    write_jsonl(lora_out_path, lora_results)

    print("Done.")
    print("Base outputs:", base_out_path)
    print("LoRA outputs:", lora_out_path)


if __name__ == "__main__":
    main()
