-- ============================================================
-- FIX: Unique constraint teacher_assignments
-- Rule cũ (SAI): (staff_id, school_year_id, semester_id, class_id)
--   → 1 giáo viên không thể dạy cùng lớp 2 lần trong 1 học kỳ
--   → SAI vì còn cho phép 2 giáo viên dạy cùng lớp + cùng môn
--
-- Rule mới (ĐÚNG): (school_year_id, semester_id, class_id, subject_id)
--   → 1 lớp + 1 môn + 1 học kỳ chỉ được phân công 1 lần (cho 1 giáo viên duy nhất)
--   → Giáo viên vẫn có thể dạy nhiều môn ở nhiều lớp
-- ============================================================

-- Bước 1: Xóa constraint cũ
ALTER TABLE teacher_assignments
    DROP CONSTRAINT IF EXISTS uk_staff_schoolyear_semester_class;

-- Bước 2: Xóa dữ liệu trùng (nếu có) trước khi thêm constraint mới
-- Giữ lại bản ghi có id nhỏ nhất cho mỗi nhóm (school_year_id, semester_id, class_id, subject_id)
DELETE FROM teacher_assignments
WHERE id NOT IN (
    SELECT MIN(id)
    FROM teacher_assignments
    WHERE class_id IS NOT NULL AND subject_id IS NOT NULL
    GROUP BY school_year_id, semester_id, class_id, subject_id
);

-- Bước 3: Thêm constraint mới
ALTER TABLE teacher_assignments
    ADD CONSTRAINT uk_schoolyear_semester_class_subject
    UNIQUE (school_year_id, semester_id, class_id, subject_id);
