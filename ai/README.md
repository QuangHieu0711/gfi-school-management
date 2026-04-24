# Chuẩn hóa dữ liệu nhận xét học bạ để fine-tune LLM

Script này dùng để đọc nhiều file Excel nhận xét học bạ tiểu học, chuẩn hóa dữ liệu, làm sạch dữ liệu và xuất ra dataset phục vụ fine-tune LLM mã nguồn mở theo hướng SFT/LoRA.

Dataset đầu ra được thiết kế cho bài toán sinh nhận xét học bạ tự động dựa trên:

- khối lớp
- môn học
- giai đoạn đánh giá
- tuần
- tiết
- mức độ học tập
- nội dung kiến thức/kỹ năng

## Cấu trúc dữ liệu đầu vào

Đặt các file Excel vào thư mục:

```text
data/raw/
```

Các tên file được hỗ trợ ánh xạ môn học:

- `DD.xlsx` -> `Đạo đức`
- `KHoc.xlsx` -> `Khoa học`
- `LSDL.xlsx` -> `Lịch sử và Địa lý`
- `T.xlsx` -> `Toán`
- `TNXH.xlsx` -> `Tự nhiên xã hội`
- `TV.xlsx` -> `Tiếng Việt`

Script ưu tiên đọc file trong `data/raw/`. Nếu thư mục này chưa có file Excel, script sẽ kiểm tra thêm các file `.xlsx` ở thư mục gốc project để hỗ trợ trường hợp bạn đang đặt file trực tiếp trong repo.

## Cài thư viện

```bash
pip install -r requirements.txt
```

## Cách chạy

```bash
python prepare_comment_dataset.py
```

## Các file output

Sau khi chạy xong, dữ liệu sẽ được tạo trong:

```text
data/processed/
```

Bao gồm:

- `dataset_clean.xlsx`: toàn bộ dữ liệu sau chuẩn hóa và làm sạch
- `stats_by_subject.xlsx`: thống kê số lượng dữ liệu theo môn học
- `stats_by_subject.png`: biểu đồ thống kê số lượng dữ liệu theo môn học
- `train.xlsx`: tập train
- `valid.xlsx`: tập validation
- `test.xlsx`: tập test
- `train.jsonl`: tập train dạng JSONL
- `valid.jsonl`: tập validation dạng JSONL
- `test.jsonl`: tập test dạng JSONL
- `all.jsonl`: toàn bộ dữ liệu dạng JSONL

## Định dạng dữ liệu dùng cho fine-tune

Mỗi dòng JSONL có dạng:

```json
{
  "instruction": "Sinh nhận xét học bạ cho học sinh tiểu học dựa trên khối lớp, môn học, giai đoạn đánh giá, mức độ học tập và nội dung kiến thức.",
  "input": "Khối: 5\nMôn: Đạo đức\nGiai đoạn: Giữa học kỳ 2\nTuần: 27\nTiết: 27\nNội dung: Phòng, tránh xâm hại",
  "output": "Em biết nêu một số cách phòng, tránh xâm hại và có ý thức bảo vệ bản thân."
}
```

## Mục đích sử dụng

Dataset này dùng để fine-tune LLM sinh nhận xét học bạ tự động cho hệ thống quản lý lớp học tiểu học.
