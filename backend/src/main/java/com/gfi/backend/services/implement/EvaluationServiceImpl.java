package com.gfi.backend.services.implement;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.evaluation.EvaluationImportResultDto;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.gfi.backend.models.dtos.evaluation.EvaluationBulkGenerateCommentRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationBulkGenerateCommentItemDto;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.evaluation.EvaluationBulkUpsertRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationEditWindowDto;
import com.gfi.backend.models.dtos.evaluation.EvaluationEditWindowRequest;
import com.gfi.backend.models.dtos.evaluation.EvaluationSheetDto;
import com.gfi.backend.models.dtos.evaluation.EvaluationSheetStudentDto;
import com.gfi.backend.models.dtos.evaluation.EvaluationStudentUpsertItemDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.Semester;
import com.gfi.backend.models.entities.Student;
import com.gfi.backend.models.entities.StudentEnrollment;
import com.gfi.backend.models.entities.StudentEvaluation;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.repositories.StudentEnrollmentRepository;
import com.gfi.backend.repositories.StudentEvaluationRepository;
import com.gfi.backend.repositories.StudentRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.services.interfaces.EvaluationService;
import com.gfi.backend.utils.SecurityUtils;

import com.gfi.backend.models.entities.ProgramDistribution;
import com.gfi.backend.models.entities.AttendanceRecord;
import com.gfi.backend.models.entities.EvaluationEditWindow;
import com.gfi.backend.models.dtos.evaluation.AiGenerateCommentRequest;
import com.gfi.backend.models.dtos.evaluation.AiGenerateCommentResponse;
import com.gfi.backend.models.dtos.evaluation.EvaluationGenerateCommentRequest;
import com.gfi.backend.repositories.AttendanceRecordRepository;
import com.gfi.backend.repositories.EvaluationEditWindowRepository;
import com.gfi.backend.repositories.ProgramDistributionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final Set<String> VALID_LEVELS = Set.of("T", "H", "C");

    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final EvaluationEditWindowRepository evaluationEditWindowRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ProgramDistributionRepository programDistributionRepository;
    private final RestTemplate restTemplate;
    private final ImportErrorFileStorageService importErrorFileStorageService;

    @Value("${ai.generate-comment.url:http://74.113.235.249:8001/generate-comment}")
    private String aiGenerateCommentUrl;

    @Override
    @Transactional(readOnly = true)
    public EvaluationSheetDto getSheet(Long classroomId, Long subjectId, Long semesterId) {
        Classroom classroom = findClassroom(classroomId);
        Subject subject = findSubject(subjectId);
        Semester semester = findSemester(semesterId);
        validateSemesterBelongsToClassroomSchoolYear(classroom, semester);

        List<StudentEnrollment> enrollments = getActiveEnrollments(classroomId);
        List<StudentEvaluation> evaluations = studentEvaluationRepository
                .findByClassroomIdAndSubjectIdAndSemesterIdAndDeletedFlagOrderByStudentIdAsc(
                        classroomId, subjectId, semesterId, 0);

        Map<Long, StudentEvaluation> evaluationByStudentId = new LinkedHashMap<>();
        for (StudentEvaluation evaluation : evaluations) {
            evaluationByStudentId.put(evaluation.getStudent().getId(), evaluation);
        }

        List<EvaluationSheetStudentDto> students = enrollments.stream()
                .map(StudentEnrollment::getStudent)
                .map(s -> {
                    StudentEvaluation e = evaluationByStudentId.get(s.getId());
                    return EvaluationSheetStudentDto.builder()
                            .studentId(s.getId())
                            .studentCode(s.getStudentCode())
                            .studentName(s.getFullName())
                            .midtermLevel(e != null ? e.getMidtermLevel() : null)
                            .midtermScore(e != null ? e.getMidtermScore() : null)
                            .midtermRemark(e != null ? e.getMidtermRemark() : null)
                            .finalLevel(e != null ? e.getFinalLevel() : null)
                            .finalScore(e != null ? e.getFinalScore() : null)
                            .finalRemark(e != null ? e.getFinalRemark() : null)
                            .build();
                })
                .toList();

        return EvaluationSheetDto.builder()
                .classroomId(classroom.getId())
                .classroomName(classroom.getName())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .semesterId(semester.getId())
                .semesterName(semester.getName())
                .students(students)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationEditWindowDto getEditWindow(Long semesterId) {
        Semester semester = findSemester(semesterId);

        return evaluationEditWindowRepository.findBySemesterIdAndDeletedFlag(semester.getId(), 0)
                .map(this::toEditWindowDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public EvaluationEditWindowDto saveEditWindow(EvaluationEditWindowRequest request) {
        Semester semester = findSemester(request.getSemesterId());
        validateEditWindowDates(request.getStartDate(), request.getEndDate());

        EvaluationEditWindow window = evaluationEditWindowRepository
                .findBySemesterIdAndDeletedFlag(semester.getId(), 0)
                .orElseGet(EvaluationEditWindow::new);

        if (window.getId() == null) {
            window.setSemester(semester);
            window.setCreatedBy(SecurityUtils.getCurrentUsername());
        }

        window.setStartDate(request.getStartDate());
        window.setEndDate(request.getEndDate());
        window.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return toEditWindowDto(evaluationEditWindowRepository.save(window));
    }

    @Override
    @Transactional
    public void bulkUpsert(EvaluationBulkUpsertRequest request) {
        Classroom classroom = findClassroom(request.getClassroomId());
        Subject subject = findSubject(request.getSubjectId());
        Semester semester = findSemester(request.getSemesterId());
        validateSemesterBelongsToClassroomSchoolYear(classroom, semester);
        validateEditWindowOpen(semester);

        for (EvaluationStudentUpsertItemDto item : request.getItems()) {
            upsertStudentEvaluation(classroom, subject, semester, item);
        }
    }

    private void upsertStudentEvaluation(Classroom classroom, Subject subject, Semester semester,
            EvaluationStudentUpsertItemDto item) {
        Student student = findStudent(item.getStudentId());
        validateStudentInClassroom(classroom.getId(), student.getId());

        Double mScore = item.getMidtermScore();
        Double fScore = item.getFinalScore();
        
        String midtermLevel = mScore != null ? calculateLevel(mScore) : normalizeLevel(item.getMidtermLevel());
        String finalLevel = fScore != null ? calculateLevel(fScore) : normalizeLevel(item.getFinalLevel());
        
        String midtermRemark = normalizeNullable(item.getMidtermRemark());
        String finalRemark = normalizeNullable(item.getFinalRemark());

        StudentEvaluation evaluation = studentEvaluationRepository
                .findByClassroomIdAndSubjectIdAndSemesterIdAndStudentId(
                        classroom.getId(), subject.getId(), semester.getId(), student.getId())
                .orElse(null);

        boolean emptyPayload = !StringUtils.hasText(midtermLevel)
                && !StringUtils.hasText(finalLevel)
                && !StringUtils.hasText(midtermRemark)
                && !StringUtils.hasText(finalRemark)
                && mScore == null
                && fScore == null;

        if (emptyPayload) {
            if (evaluation != null) {
                evaluation.setDeletedFlag(1);
                evaluation.setDeletedAt(LocalDateTime.now());
                evaluation.setDeletedBy(SecurityUtils.getCurrentUsername());
                studentEvaluationRepository.save(evaluation);
            }
            return;
        }

        if (evaluation == null) {
            evaluation = new StudentEvaluation();
            evaluation.setClassroom(classroom);
            evaluation.setSubject(subject);
            evaluation.setSemester(semester);
            evaluation.setStudent(student);
            evaluation.setCreatedBy(SecurityUtils.getCurrentUsername());
            evaluation.setDeletedFlag(0);
        } else {
            evaluation.setUpdatedBy(SecurityUtils.getCurrentUsername());
            evaluation.setDeletedFlag(0);
            evaluation.setDeletedAt(null);
            evaluation.setDeletedBy(null);
        }

        evaluation.setMidtermLevel(midtermLevel);
        evaluation.setMidtermScore(mScore);
        evaluation.setMidtermRemark(midtermRemark);
        evaluation.setFinalLevel(finalLevel);
        evaluation.setFinalScore(fScore);
        evaluation.setFinalRemark(finalRemark);
        studentEvaluationRepository.save(evaluation);
    }

    private String calculateLevel(Double score) {
        if (score == null) return null;
        if (score >= 9) return "T";
        if (score >= 5) return "H";
        return "C";
    }

    @Override
    @Transactional(readOnly = true)
    public String generateComment(EvaluationGenerateCommentRequest request) {
        Classroom classroom = findClassroom(request.getClassroomId());
        Subject subject = findSubject(request.getSubjectId());
        Student student = findStudent(request.getStudentId());

        String className = classroom.getName();
        String gradeLevelName = classroom.getGradeLevel().getName();
        if (gradeLevelName == null || !gradeLevelName.toLowerCase().startsWith("lớp")) {
            gradeLevelName = "Lớp " + classroom.getGradeLevel().getGradeNumber();
        }

        String subjectName = subject.getName();

        int[] weekRange = getWeekRangeForTerm(request.getTerm());

        List<ProgramDistribution> ppcts = programDistributionRepository
                .findBySchoolYearIdAndUnitIdAndClassroomIdAndSubjectIdAndDeletedFlagOrderByOrderNumberAscIdAsc(
                        classroom.getSchoolYear().getId(),
                        classroom.getUnit().getId(),
                        classroom.getId(),
                        subject.getId(),
                        0
                );

        List<ProgramDistribution> validPpcts = new java.util.ArrayList<>();
        for (ProgramDistribution p : ppcts) {
            if (p.getWeekNumber() != null && p.getWeekNumber() >= weekRange[0] && p.getWeekNumber() <= weekRange[1]) {
                validPpcts.add(p);
            }
        }
        
        ProgramDistribution selectedPpct = null;
        if (!validPpcts.isEmpty()) {
            java.util.Random random = new java.util.Random();
            selectedPpct = validPpcts.get(random.nextInt(validPpcts.size()));
        }

        String lessonTitle = selectedPpct != null ? selectedPpct.getLessonName() : "Bài ôn tập";
        Integer weekNo = selectedPpct != null ? selectedPpct.getWeekNumber() : weekRange[0];
        Integer lessonNo = selectedPpct != null ? selectedPpct.getOrderNumber() : 1;
        String learningObjective = selectedPpct != null && selectedPpct.getNote() != null ? selectedPpct.getNote() : ""; 
        String textbookSeries = "KẾT NỐI TRI THỨC VỚI CUỘC SỐNG";

        List<AttendanceRecord> attendances = attendanceRecordRepository
                .findByClassroomIdAndStudentIdAndDeletedFlag(classroom.getId(), student.getId(), 0);

        long countC = 0, countP = 0, countK = 0, countX = 0;
        for (AttendanceRecord r : attendances) {
            if ("C".equalsIgnoreCase(r.getAttendanceStatus())) countC++;
            else if ("P".equalsIgnoreCase(r.getAttendanceStatus())) countP++;
            else if ("K".equalsIgnoreCase(r.getAttendanceStatus())) countK++;
            else if ("X".equalsIgnoreCase(r.getAttendanceStatus())) countX++;
        }

        String attendanceCode = "C";
        String attendanceStatus = "Có mặt";

        if (countK > 0) {
            attendanceCode = "K";
            attendanceStatus = "Nghỉ không phép";
        } else if (countX > 0) {
            attendanceCode = "X";
            attendanceStatus = "Trường hợp khác";
        } else if (countP > 0) {
            attendanceCode = "P";
            attendanceStatus = "Nghỉ có phép";
        } else {
            attendanceCode = "C";
            attendanceStatus = "Có mặt";
        }

        String rawEvaluation = request.getEvaluation();
        String mappedEvaluation = rawEvaluation;
        if ("T".equalsIgnoreCase(rawEvaluation)) {
            mappedEvaluation = "Tốt";
        } else if ("H".equalsIgnoreCase(rawEvaluation)) {
            mappedEvaluation = "Hoàn thành";
        } else if ("C".equalsIgnoreCase(rawEvaluation)) {
            mappedEvaluation = "Cần cố gắng";
        }

        AiGenerateCommentRequest aiRequest = AiGenerateCommentRequest.builder()
                .gradeLevel(gradeLevelName)
                .subjectName(subjectName)
                .term(request.getTerm())
                .weekNo(weekNo)
                .lessonNo(lessonNo)
                .lessonTitle(lessonTitle)
                .learningObjective(learningObjective)
                .evaluation(mappedEvaluation)
                .attendanceStatus(attendanceStatus)
                .attendanceCode(attendanceCode)
                .participationLevel(request.getParticipationLevel())
                .behaviorTag(request.getBehaviorTag())
                .textbookSeries(textbookSeries)
                .build();

        try {
            AiGenerateCommentResponse response = callAiService(aiRequest);
            return response != null && response.getComment() != null ? response.getComment() : "";
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserMessageException(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Lỗi khi gọi AI service: " + e.getMessage());
        }
    }

    private AiGenerateCommentResponse callAiService(AiGenerateCommentRequest aiRequest) {
        String configuredUrl = normalizeNullable(aiGenerateCommentUrl);
        if (!StringUtils.hasText(configuredUrl)) {
            throw new RuntimeException("AI_GENERATE_COMMENT_URL chua duoc cau hinh");
        }

        return restTemplate.postForObject(configuredUrl, aiRequest, AiGenerateCommentResponse.class);

        /* legacy fallback removed
            throw new RuntimeException(
                    "Không thể kết nối AI service. Đã thử các URL: " + String.join(", ", candidateUrls),
                    lastException);
        }

        throw new RuntimeException("Không thể kết nối tới AI service");
    }

        */
    }
    private List<String> getAiCandidateUrls() {
        String configuredUrl = normalizeNullable(aiGenerateCommentUrl);
        if (!StringUtils.hasText(configuredUrl)) {
            return List.of(
                    "http://74.113.235.249:8001/generate-comment",
                    "http://127.0.0.1:8001/generate-comment",
                    "http://gfi-ai:8001/generate-comment");
        }

        List<String> candidates = new java.util.ArrayList<>();
        candidates.add(configuredUrl);

        try {
            URI uri = URI.create(configuredUrl);
            String host = uri.getHost();
            if ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)) {
                candidates.add(replaceHost(uri, "gfi-ai"));
            } else if ("gfi-ai".equalsIgnoreCase(host)) {
                candidates.add(replaceHost(uri, "127.0.0.1"));
            }
        } catch (IllegalArgumentException ignored) {
            // Keep only the configured URL when it cannot be parsed safely.
        }

        return candidates.stream().distinct().toList();
    }

    private String replaceHost(URI uri, String newHost) {
        return URI.create(String.format("%s://%s%s%s",
                uri.getScheme(),
                newHost,
                uri.getPort() >= 0 ? ":" + uri.getPort() : "",
                uri.getRawPath() != null ? uri.getRawPath() : ""))
                .toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> bulkGenerateComment(EvaluationBulkGenerateCommentRequest request) {
        Map<Long, String> result = new java.util.concurrent.ConcurrentHashMap<>();
        Queue<String> errors = new ConcurrentLinkedQueue<>();

        request.getItems().parallelStream().forEach(item -> {
            EvaluationGenerateCommentRequest singleReq = new EvaluationGenerateCommentRequest();
            singleReq.setClassroomId(request.getClassroomId());
            singleReq.setSubjectId(request.getSubjectId());
            singleReq.setTerm(request.getTerm());
            singleReq.setStudentId(item.getStudentId());
            singleReq.setEvaluation(item.getEvaluation());
            singleReq.setParticipationLevel(item.getParticipationLevel());
            singleReq.setBehaviorTag(item.getBehaviorTag());

            try {
                String comment = generateComment(singleReq);
                result.put(item.getStudentId(), comment);
            } catch (UserMessageException e) {
                errors.add(String.format("studentId=%d: %s", item.getStudentId(), e.getMessage()));
            } catch (Exception e) {
                errors.add(String.format("studentId=%d: %s", item.getStudentId(), e.getMessage()));
            }
        });

        if (!errors.isEmpty()) {
            String firstError = errors.peek();
            throw new UserMessageException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                    "Khong the sinh nhan xet hang loat. " + firstError);
        }

        return result;
    }

    private int[] getWeekRangeForTerm(String term) {
        if (term == null) return new int[]{1, 35};
        switch (term.toUpperCase()) {
            case "GK1": return new int[]{1, 9};
            case "CK1": return new int[]{10, 18};
            case "GK2": return new int[]{19, 27};
            case "CK2": return new int[]{28, 35};
            default: return new int[]{1, 35};
        }
    }

    private List<StudentEnrollment> getActiveEnrollments(Long classroomId) {
        return studentEnrollmentRepository.findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0);
    }

    private void validateSemesterBelongsToClassroomSchoolYear(Classroom classroom, Semester semester) {
        if (!semester.getSchoolYear().getId().equals(classroom.getSchoolYear().getId())) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Hoc ky khong thuoc nam hoc cua lop");
        }
    }

    private void validateStudentInClassroom(Long classroomId, Long studentId) {
        boolean exists = studentEnrollmentRepository
                .findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0)
                .stream()
                .anyMatch(enrollment -> enrollment.getStudent().getId().equals(studentId));
        if (!exists) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Hoc sinh khong thuoc lop da chon");
        }
    }

    private String normalizeLevel(String level) {
        String normalized = normalize(level);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ("1".equals(normalized)) {
            normalized = "T";
        } else if ("2".equals(normalized)) {
            normalized = "H";
        } else if ("3".equals(normalized)) {
            normalized = "C";
        }
        if (!VALID_LEVELS.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Muc danh gia chi ho tro T, H, C hoac 1, 2, 3");
        }
        return normalized;
    }

    private void validateEditWindowDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new UserMessageException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateEditWindowOpen(Semester semester) {
        EvaluationEditWindow window = evaluationEditWindowRepository
                .findBySemesterIdAndDeletedFlag(semester.getId(), 0)
                .orElseThrow(() -> new UserMessageException(
                        CommonErrorCode.INVALID_REQUEST.getCode(),
                        "Chưa cấu hình thời gian sửa điểm cho học kỳ này."));

        LocalDate today = LocalDate.now();
        if (today.isBefore(window.getStartDate()) || today.isAfter(window.getEndDate())) {
            throw new UserMessageException(
                    CommonErrorCode.INVALID_REQUEST.getCode(),
                    "Hiện không nằm trong thời gian được phép sửa điểm.");
        }
    }

    private EvaluationEditWindowDto toEditWindowDto(EvaluationEditWindow window) {
        return EvaluationEditWindowDto.builder()
                .id(window.getId())
                .semesterId(window.getSemester().getId())
                .startDate(window.getStartDate())
                .endDate(window.getEndDate())
                .createdAt(window.getCreatedAt())
                .updatedAt(window.getUpdatedAt())
                .build();
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
    }

    private Semester findSemester(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SEMESTER_NOT_FOUND));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STUDENT_NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate(Long classroomId, Long subjectId, Long semesterId) {
        Classroom classroom = findClassroom(classroomId);
        Subject subject = findSubject(subjectId);
        Semester semester = findSemester(semesterId);

        List<StudentEnrollment> enrollments = getActiveEnrollments(classroomId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DanhGia");

            // Title
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(35);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("MẪU IMPORT ĐÁNH GIÁ HỌC SINH");
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setFontName("Times New Roman");
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            // Info row
            Row infoRow = sheet.createRow(1);
            infoRow.setHeightInPoints(25);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(String.format("Lớp: %s | Môn: %s | Học kỳ: %s", 
                    classroom.getName(), subject.getName(), semester.getName()));
            CellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setAlignment(HorizontalAlignment.CENTER);
            infoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font infoFont = workbook.createFont();
            infoFont.setItalic(true);
            infoFont.setFontName("Times New Roman");
            infoStyle.setFont(infoFont);
            infoCell.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            // Header
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(30);
            String[] headers = { "STT", "Mã HS", "Họ và tên", "Điểm GK", "Mức GK", "Nhận xét GK", "Điểm CK", "Mức CK", "Nhận xét CK", "StudentId (Không sửa)" };
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE1.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName("Times New Roman");
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Body Style
            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font bodyFont = workbook.createFont();
            bodyFont.setFontName("Times New Roman");
            bodyStyle.setFont(bodyFont);

            // Populate students
            int rowIndex = 4;
            int stt = 1;
            for (StudentEnrollment enrollment : enrollments) {
                Student s = enrollment.getStudent();
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(25);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(s.getStudentCode());
                row.createCell(2).setCellValue(s.getFullName());
                row.createCell(3).setCellValue(""); // Điểm GK
                row.createCell(4).setCellValue(""); // Mức GK
                row.createCell(5).setCellValue(""); // Nhận xét GK
                row.createCell(6).setCellValue(""); // Điểm CK
                row.createCell(7).setCellValue(""); // Mức CK
                row.createCell(8).setCellValue(""); // Nhận xét CK
                row.createCell(9).setCellValue(s.getId()); // Hidden ID

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(bodyStyle);
                }
            }

            // Column Widths
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 30 * 256);
            sheet.setColumnWidth(3, 10 * 256);
            sheet.setColumnWidth(4, 10 * 256);
            sheet.setColumnWidth(5, 30 * 256);
            sheet.setColumnWidth(6, 10 * 256);
            sheet.setColumnWidth(7, 10 * 256);
            sheet.setColumnWidth(8, 30 * 256);
            sheet.setColumnWidth(9, 0); // Hide StudentId

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UserMessageException("Lỗi khi tạo file mẫu");
        }
    }

    @Override
    @Transactional
    public EvaluationImportResultDto importExcel(MultipartFile file, Long classroomId, Long subjectId, Long semesterId) {
        if (file == null || file.isEmpty()) throw new UserMessageException("File không hợp lệ");
        
        Classroom classroom = findClassroom(classroomId);
        Subject subject = findSubject(subjectId);
        Semester semester = findSemester(semesterId);
        validateSemesterBelongsToClassroomSchoolYear(classroom, semester);
        validateEditWindowOpen(semester);

        int successCount = 0;
        int failedCount = 0;
        Map<Integer, String> rowErrors = new LinkedHashMap<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            
            // Assume data starts from row 4 (index 4)
            for (int rowIndex = 4; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String studentIdStr = readCellText(row.getCell(9), formatter);
                if (!StringUtils.hasText(studentIdStr)) continue;

                try {
                    Long studentId = Long.parseLong(studentIdStr);
                    
                    Double gkScore = parseDouble(readCellText(row.getCell(3), formatter));
                    String gkLevel = readCellText(row.getCell(4), formatter);
                    String gkRemark = readCellText(row.getCell(5), formatter);
                    
                    Double ckScore = parseDouble(readCellText(row.getCell(6), formatter));
                    String ckLevel = readCellText(row.getCell(7), formatter);
                    String ckRemark = readCellText(row.getCell(8), formatter);

                    if (gkScore == null && !StringUtils.hasText(gkLevel) && !StringUtils.hasText(gkRemark)
                        && ckScore == null && !StringUtils.hasText(ckLevel) && !StringUtils.hasText(ckRemark)) {
                        continue;
                    }

                    EvaluationStudentUpsertItemDto item = EvaluationStudentUpsertItemDto.builder()
                            .studentId(studentId)
                            .midtermScore(gkScore)
                            .midtermLevel(gkLevel) 
                            .midtermRemark(gkRemark)
                            .finalScore(ckScore)
                            .finalLevel(ckLevel) 
                            .finalRemark(ckRemark)
                            .build();

                    upsertStudentEvaluation(classroom, subject, semester, item);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    rowErrors.put(rowIndex, e instanceof UserMessageException ? e.getMessage() : "Lỗi xử lý dòng");
                }
            }

            String errorFileToken = null;
            String errorFileName = null;
            if (!rowErrors.isEmpty()) {
                byte[] errorFileContent = buildImportErrorFile(workbook, rowErrors);
                errorFileName = "evaluation_import_errors_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
                errorFileToken = importErrorFileStorageService.store(errorFileName, 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                        errorFileContent);
            }

            return EvaluationImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .hasErrorFile(!rowErrors.isEmpty())
                    .errorFileName(errorFileName)
                    .errorFileToken(errorFileToken)
                    .build();

        } catch (IOException e) {
            throw new UserMessageException("Lỗi đọc file Excel");
        }
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }

    private byte[] buildImportErrorFile(Workbook workbook, Map<Integer, String> rowErrors) {
        Sheet sheet = workbook.getSheetAt(0);
        int errorCol = 10;
        
        Row headerRow = sheet.getRow(3);
        Cell errorHeader = headerRow.createCell(errorCol);
        errorHeader.setCellValue("Lỗi chi tiết");
        
        CellStyle headerStyle = headerRow.getCell(0).getCellStyle();
        errorHeader.setCellStyle(headerStyle);
        
        CellStyle errorStyle = workbook.createCellStyle();
        errorStyle.cloneStyleFrom(sheet.getRow(4).getCell(0).getCellStyle());
        Font errorFont = workbook.createFont();
        errorFont.setColor(IndexedColors.RED.getIndex());
        errorFont.setFontName("Times New Roman");
        errorStyle.setFont(errorFont);

        rowErrors.forEach((rowIndex, error) -> {
            Row row = sheet.getRow(rowIndex);
            Cell cell = row.createCell(errorCol);
            cell.setCellValue(error);
            cell.setCellStyle(errorStyle);
        });

        sheet.setColumnWidth(errorCol, 45 * 256);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private String readCellText(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
