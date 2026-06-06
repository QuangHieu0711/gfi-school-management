# Báo cáo So sánh Kết quả Sinh Nhận xét Học bạ

Báo cáo này so sánh kết quả sinh câu nhận xét tự động giữa **Mô hình gốc (Qwen2.5-1.5B-Instruct)** và **Mô hình sau khi Fine-tune LoRA (GFI Comment AI)**.

| STT | Tình huống kiểm thử | Nhận xét từ Mô hình gốc (Base Model) | Nhận xét từ Mô hình Fine-tune (LoRA) |
| :--- | :--- | :--- | :--- |
| 1 | **Toán - Lớp 1 - Tốt** | Em rất hài lòng với sự tiến bộ của học sinh trong việc học Toán, đặc biệt là trong tuần này khi thực hiện các bài tập về phép cộng trong phạm vi 10. | Em thực hiện tốt phép cộng, biết kiểm tra kết quả và hoàn thành bài học khá chắc. |
| 2 | **Tiếng Việt - Lớp 2 - Hoàn thành** | Em rất hài lòng với sự cố gắng và tiến bộ của học sinh trong việc học tập môn Tiếng Việt cuối học kỳ 1. | Em hoàn thành bài viết theo yêu cầu, cần cẩn thận hơn khi thao tác. |
| 3 | **Giáo dục thể chất - Lớp 3 - Chưa hoàn thành** | Em thấy học sinh chưa hoàn thành mục tiêu bài học, mặc dù đã tham gia và có thái độ cần cố gắng, nhưng chưa thực hiện được động tác vươn thở và động tác | Em cần tập trung hơn khi luyện các động tác vươn thở và tay để thực hiện đúng yêu cầu. |
| 4 | **Đạo đức - Lớp 4 - Tốt** | Em rất hài lòng với sự cố gắng và thái độ của học sinh trong bài học. | Em thực hiện tốt việc lễ phép, kính trọng người lao động, biết chia sẻ và làm gương cho bạn. |
| 5 | **Tự nhiên và Xã hội - Lớp 5 - Hoàn thành** | Em rất hài lòng với cách học của bạn trong tiết học này. | Em thực hiện quy định khi tham gia giao thông, cần chủ động hơn. |

## Nhận xét chi tiết

### Mô hình gốc (Qwen2.5-1.5B-Instruct)
- Thường sinh câu nhận xét dài hơn yêu cầu.
- Có xu hướng lặp lại nội dung từ prompt đầu vào.
- Một số trường hợp xuất hiện câu chưa hoàn chỉnh hoặc diễn đạt chưa tự nhiên.
- Đôi khi xuất hiện từ ngữ không phù hợp với văn phong nhận xét học sinh tiểu học.

### Mô hình sau Fine-tune LoRA (GFI Comment AI)
- Sinh nhận xét ngắn gọn và bám sát yêu cầu nghiệp vụ hơn.
- Phần lớn kết quả bắt đầu bằng từ "Em" và phù hợp với văn phong nhận xét học sinh tiểu học.
- Nội dung nhận xét có liên hệ rõ hơn với môn học, mục tiêu bài học và mức đánh giá đầu vào.
- Hạn chế được hiện tượng lặp lại prompt và sinh câu quá dài so với mô hình gốc.
- Tuy nhiên vẫn cần tiếp tục kiểm thử trên tập dữ liệu lớn hơn để đánh giá mức độ ổn định và khả năng tổng quát hóa của mô hình.
