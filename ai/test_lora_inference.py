#!/usr/bin/env python3
"""
Compare base Qwen model and a LoRA adapter by running a set of Vietnamese prompts.

Usage examples:
  python test_lora_inference.py --adapter-path "E:/DO_AN_GIF/ai/gfi_comments_lora_final/saves/qwen2_5_1_5b/gfi_comments_lora" --device cpu

The script will write `outputs/base_outputs.jsonl` and `outputs/lora_outputs.jsonl`.
"""
import argparse
import json
import os
import sys
import gc

def check_imports():
    missing = []
    try:
        import torch
    except Exception:
        missing.append("torch")
    try:
        import transformers
    except Exception:
        missing.append("transformers")
    try:
        import peft
    except Exception:
        missing.append("peft")
    if missing:
        print("Missing required packages:", ", ".join(missing))
        print("Install them in your venv, for example:")
        print("  pip install torch transformers accelerate peft")
        sys.exit(2)


def load_tokenizer_and_model(model_name, device, low_cpu_mem_usage=True):
    from transformers import AutoTokenizer, AutoModelForCausalLM
    import torch

    tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)

    kwargs = dict(trust_remote_code=True)
    if device == "cuda":
        kwargs.update(dict(device_map="auto", torch_dtype=torch.float16))
    else:
        # keep memory usage lower on CPU when possible
        kwargs.update(dict(device_map=None))
        if low_cpu_mem_usage:
            kwargs.update(dict(low_cpu_mem_usage=True))

    model = AutoModelForCausalLM.from_pretrained(model_name, **kwargs)

    if device == "cpu":
        model.to("cpu")

    return tokenizer, model


def generate_for_prompts(tokenizer, model, prompts, max_new_tokens=256, device="cpu"):
    import torch

    results = []
    for p in prompts:
        inputs = tokenizer(p, return_tensors="pt")
        inputs = {k: v.to(model.device) for k, v in inputs.items()}
        with torch.no_grad():
            out = model.generate(**inputs, max_new_tokens=max_new_tokens)
        text = tokenizer.decode(out[0], skip_special_tokens=True)
        results.append({"prompt": p, "response": text})
    return results


def main():
    check_imports()

    parser = argparse.ArgumentParser()
    parser.add_argument("--base-model", default="Qwen/Qwen2.5-1.5B-Instruct")
    parser.add_argument("--adapter-path", required=True)
    parser.add_argument("--device", choices=["cpu", "cuda"], default=("cuda" if (os.environ.get("CUDA_VISIBLE_DEVICES") or False) else "cpu"))
    parser.add_argument("--out-dir", default="outputs")
    args = parser.parse_args()

    adapter_path = args.adapter_path
    if not os.path.isdir(adapter_path):
        print("Adapter path does not exist:", adapter_path)
        sys.exit(3)

    prompts = [
        "Viết nhận xét cuối tháng cho học sinh Nguyễn Văn A. Học sinh chăm ngoan, đi học đều, tiếp thu bài khá, cần rèn thêm chữ viết.",
        "Viết nhận xét cho học sinh giỏi: Phan Thị B — học lực giỏi, tham gia tích cực, thiệt hại ít.",
        "Viết nhận xét cho học sinh khá: Lê Văn C — tiếp thu tốt, cần phát huy tinh thần tự học.",
        "Viết nhận xét cho học sinh trung bình: Trần Văn D — học lực trung bình, cần cải thiện tập trung trên lớp.",
        "Viết nhận xét cho học sinh còn nghịch: Nguyễn Thị E — hành vi nghịch ngợm, cần rèn kỷ luật.",
        "Viết nhận xét cho học sinh nghỉ học nhiều: Hoàng Văn F — nghỉ học thường xuyên, yêu cầu gia đình phối hợp.",
        "Viết nhận xét cho học sinh chữ viết yếu: Bùi Thị G — chữ viết cần rèn thêm, đề nghị luyện chữ.",
        "Viết nhận xét cho học sinh tiến bộ: Phạm Văn H — tiến bộ rõ rệt, cần duy trì nỗ lực.",
        "Viết nhận xét ngắn gọn, lịch sự cho học sinh trung bình nhưng có tinh thần cố gắng.",
        "Viết nhận xét thân thiện, khích lệ cho học sinh chưa hoàn thành bài tập.",
        "Viết nhận xét mang tính cảnh báo nhẹ cho học sinh vi phạm nội quy lớp.",
        "Viết nhận xét chi tiết về thái độ học tập và kỹ năng tự quản cho một học sinh.",
    ]

    os.makedirs(args.out_dir, exist_ok=True)

    print("Loading base model (this may take a while)...")
    tokenizer, base_model = load_tokenizer_and_model(args.base_model, args.device)
    print("Generating with base model...")
    base_results = generate_for_prompts(tokenizer, base_model, prompts, device=args.device)
    base_out_path = os.path.join(args.out_dir, "base_outputs.jsonl")
    with open(base_out_path, "w", encoding="utf-8") as f:
        for r in base_results:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")

    # free memory before loading adapter version
    try:
        del base_model
        gc.collect()
        import torch
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
    except Exception:
        pass

    print("Loading base model again and attaching LoRA adapter (this may take a while)...")
    tokenizer, model_for_lora = load_tokenizer_and_model(args.base_model, args.device)
    print("Applying LoRA adapter from:", adapter_path)
    from peft import PeftModel
    model_lora = PeftModel.from_pretrained(model_for_lora, adapter_path, device_map=("auto" if args.device == "cuda" else None))

    print("Generating with LoRA model...")
    lora_results = generate_for_prompts(tokenizer, model_lora, prompts, device=args.device)
    lora_out_path = os.path.join(args.out_dir, "lora_outputs.jsonl")
    with open(lora_out_path, "w", encoding="utf-8") as f:
        for r in lora_results:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")

    print("Done.")
    print("Base outputs:", base_out_path)
    print("LoRA outputs:", lora_out_path)


if __name__ == "__main__":
    main()
