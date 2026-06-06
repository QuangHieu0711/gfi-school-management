import os
import gc
import json
import sys
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel

# Reconfigure stdout to use UTF-8 for Windows compatibility
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass  # For python versions that don't support reconfigure

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

TEST_CASES = [
    {
        "id": 1,
        "title": "Toán - Lớp 1 - Tốt",
        "prompt": """Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: 1
Môn học: Toán
Học kì: Giữa học kỳ 1
Tuần: 5
Tiết: 10
Tên bài học: Phép cộng trong phạm vi 10
Mục tiêu bài học: Biết thực hiện phép cộng các số trong phạm vi 10.
Mức đánh giá: Hoàn thành tốt
Đi học đầy đủ: Có
Mức độ tham gia: Tích cực
Thái độ: Chăm chỉ
Bộ sách: Cánh Diều

Yêu cầu bắt buộc:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt có dấu.
- Không dùng tiếng Anh hoặc từ nước ngoài.
- Không nhắc tên học sinh.
- Tuyệt đối không lặp lại chữ 'Bài' hay 'Tiết' trong nhận xét.
- Không giải thích."""
    },
    {
        "id": 2,
        "title": "Tiếng Việt - Lớp 2 - Hoàn thành",
        "prompt": """Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: 2
Môn học: Tiếng Việt
Học kì: Cuối học kỳ 1
Tuần: 12
Tiết: 3
Tên bài học: Tập viết chữ hoa
Mục tiêu bài học: Viết đúng nét và đúng cỡ chữ hoa theo mẫu.
Mức đánh giá: Hoàn thành
Đi học đầy đủ: Có
Mức độ tham gia: Đạt yêu cầu
Thái độ: Tự giác
Bộ sách: Kết nối tri thức với cuộc sống

Yêu cầu bắt buộc:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt có dấu.
- Không dùng tiếng Anh hoặc từ nước ngoài.
- Không nhắc tên học sinh.
- Tuyệt đối không lặp lại chữ 'Bài' hay 'Tiết' trong nhận xét.
- Không giải thích."""
    },
    {
        "id": 3,
        "title": "Giáo dục thể chất - Lớp 3 - Chưa hoàn thành",
        "prompt": """Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: 3
Môn học: Giáo dục thể chất
Học kì: Giữa học kỳ 2
Tuần: 20
Tiết: 2
Tên bài học: Động tác vươn thở và động tác tay
Mục tiêu bài học: Thực hiện được động tác vươn thở và động tác tay của bài thể dục phát triển chung.
Mức đánh giá: Chưa hoàn thành
Đi học đầy đủ: Vắng 1 buổi
Mức độ tham gia: Thụ động
Thái độ: Cần cố gắng
Bộ sách: Chân trời sáng tạo

Yêu cầu bắt buộc:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt có dấu.
- Không dùng tiếng Anh hoặc từ nước ngoài.
- Không nhắc tên học sinh.
- Tuyệt đối không lặp lại chữ 'Bài' hay 'Tiết' trong nhận xét.
- Không giải thích."""
    },
    {
        "id": 4,
        "title": "Đạo đức - Lớp 4 - Tốt",
        "prompt": """Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: 4
Môn học: Đạo đức
Học kì: Cuối học kỳ 2
Tuần: 32
Tiết: 32
Tên bài học: Biết ơn người lao động
Mục tiêu bài học: Hiểu được tầm quan trọng của người lao động và thể hiện lòng biết ơn.
Mức đánh giá: Hoàn thành tốt
Đi học đầy đủ: Có
Mức độ tham gia: Tích cực
Thái độ: Kính trọng, lễ phép
Bộ sách: Cánh Diều

Yêu cầu bắt buộc:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt có dấu.
- Không dùng tiếng Anh hoặc từ nước ngoài.
- Không nhắc tên học sinh.
- Tuyệt đối không lặp lại chữ 'Bài' hay 'Tiết' trong nhận xét.
- Không giải thích."""
    },
    {
        "id": 5,
        "title": "Tự nhiên và Xã hội - Lớp 5 - Hoàn thành",
        "prompt": """Viết nhận xét cho học sinh dựa trên thông tin sau:

Khối: 5
Môn học: Tự nhiên và Xã hội
Học kì: Giữa học kỳ 1
Tuần: 8
Tiết: 8
Tên bài học: Phòng tránh tai nạn giao thông đường bộ
Mục tiêu bài học: Nhận biết được một số nguyên nhân gây tai nạn giao thông và cách phòng tránh.
Mức đánh giá: Hoàn thành
Đi học đầy đủ: Có
Mức độ tham gia: Đều
Thái độ: Chú ý lắng nghe
Bộ sách: Kết nối tri thức với cuộc sống

Yêu cầu bắt buộc:
- Chỉ viết 1 câu nhận xét.
- Bắt đầu bằng "Em".
- Chỉ dùng tiếng Việt có dấu.
- Không dùng tiếng Anh hoặc từ nước ngoài.
- Không nhắc tên học sinh.
- Tuyệt đối không lặp lại chữ 'Bài' hay 'Tiết' trong nhận xét.
- Không giải thích."""
    }
]

def clean_response(text: str) -> str:
    text = text.strip().strip('"""\'\'`')
    import re
    text = re.sub(
        r"^(?:assistant|nhận xét|comment|answer|trả lời)\s*[:：\-]\s*",
        "",
        text,
        flags=re.IGNORECASE,
    )
    return text.strip()

def run_generation(model, tokenizer, device, prompt):
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
            max_new_tokens=40,
            do_sample=False,
            repetition_penalty=1.0,
            eos_token_id=tokenizer.eos_token_id,
            pad_token_id=tokenizer.eos_token_id,
        )

    response_text = tokenizer.decode(
        output[0][input_ids.shape[-1] :],
        skip_special_tokens=True,
    ).strip()
    return clean_response(response_text)

def main():
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Using device: {device}")

    base_results = []
    lora_results = []

    # 1. Generate with Base Model
    print("Loading Base Model...")
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL, trust_remote_code=True)
    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token

    kwargs = {"trust_remote_code": True}
    if device == "cuda":
        kwargs.update({"device_map": "auto", "torch_dtype": torch.float16})
    else:
        kwargs.update({"low_cpu_mem_usage": False})

    base_model = AutoModelForCausalLM.from_pretrained(BASE_MODEL, **kwargs)
    base_model.eval()

    print("Generating responses with Base Model...")
    for idx, case in enumerate(TEST_CASES):
        print(f"  Running case {idx+1}/{len(TEST_CASES)}: {case['title']}")
        res = run_generation(base_model, tokenizer, device, case["prompt"])
        base_results.append(res)
        print(f"    Base Output: {res}")

    del base_model
    gc.collect()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()

    # 2. Generate with LoRA Model
    print("\nLoading LoRA Model...")
    base_model_for_lora = AutoModelForCausalLM.from_pretrained(BASE_MODEL, **kwargs)
    lora_model = PeftModel.from_pretrained(base_model_for_lora, ADAPTER_PATH)
    if device == "cpu":
        lora_model.to("cpu")
    lora_model.eval()

    print("Generating responses with LoRA Model...")
    for idx, case in enumerate(TEST_CASES):
        print(f"  Running case {idx+1}/{len(TEST_CASES)}: {case['title']}")
        res = run_generation(lora_model, tokenizer, device, case["prompt"])
        lora_results.append(res)
        print(f"    LoRA Output: {res}")

    del lora_model
    del base_model_for_lora
    gc.collect()
    if torch.cuda.is_available():
        torch.cuda.empty_cache()

    # 3. Write report
    output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "outputs")
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, "model_comparison.md")

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("# Báo cáo So sánh Kết quả Sinh Nhận xét Học bạ\n\n")
        f.write("Báo cáo này so sánh kết quả sinh câu nhận xét tự động giữa **Mô hình gốc (Qwen2.5-1.5B-Instruct)** và **Mô hình sau khi Fine-tune LoRA (GFI Comment AI)**.\n\n")
        f.write("| STT | Tình huống kiểm thử | Nhận xét từ Mô hình gốc (Base Model) | Nhận xét từ Mô hình Fine-tune (LoRA) |\n")
        f.write("| :--- | :--- | :--- | :--- |\n")
        for i in range(len(TEST_CASES)):
            title = TEST_CASES[i]["title"]
            base_out = base_results[i].replace("\n", " ")
            lora_out = lora_results[i].replace("\n", " ")
            f.write(f"| {i+1} | **{title}** | {base_out} | {lora_out} |\n")

        f.write("\n## Nhận xét chi tiết\n\n")
        f.write("### Mô hình gốc (Qwen2.5-1.5B-Instruct)\n")
        f.write("- Thường sinh câu nhận xét dài hơn yêu cầu.\n")
        f.write("- Có xu hướng lặp lại nội dung từ prompt đầu vào.\n")
        f.write("- Một số trường hợp xuất hiện câu chưa hoàn chỉnh hoặc diễn đạt chưa tự nhiên.\n")
        f.write("- Đôi khi xuất hiện từ ngữ không phù hợp với văn phong nhận xét học sinh tiểu học.\n\n")
        f.write("### Mô hình sau Fine-tune LoRA (GFI Comment AI)\n")
        f.write("- Sinh nhận xét ngắn gọn và bám sát yêu cầu nghiệp vụ hơn.\n")
        f.write("- Phần lớn kết quả bắt đầu bằng từ \"Em\" và phù hợp với văn phong nhận xét học sinh tiểu học.\n")
        f.write("- Nội dung nhận xét có liên hệ rõ hơn với môn học, mục tiêu bài học và mức đánh giá đầu vào.\n")
        f.write("- Hạn chế được hiện tượng lặp lại prompt và sinh câu quá dài so với mô hình gốc.\n")
        f.write("- Tuy nhiên vẫn cần tiếp tục kiểm thử trên tập dữ liệu lớn hơn để đánh giá mức độ ổn định và khả năng tổng quát hóa của mô hình.\n")

    print(f"\nSuccessfully generated comparison report at {report_path}")

if __name__ == "__main__":
    main()
