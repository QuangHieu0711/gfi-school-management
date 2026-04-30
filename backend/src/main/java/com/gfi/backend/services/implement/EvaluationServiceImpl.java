package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.evaluation.EvaluationBulkUpsertRequest;
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
import com.gfi.backend.models.dtos.evaluation.AiGenerateCommentRequest;
import com.gfi.backend.models.dtos.evaluation.AiGenerateCommentResponse;
import com.gfi.backend.models.dtos.evaluation.EvaluationGenerateCommentRequest;
import com.gfi.backend.repositories.AttendanceRecordRepository;
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
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ProgramDistributionRepository programDistributionRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.generate-comment.url:http://127.0.0.1:8001/generate-comment}")
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
                .map(student -> {
                    StudentEvaluation evaluation = evaluationByStudentId.get(student.getId());
                    return EvaluationSheetStudentDto.builder()
                            .studentId(student.getId())
                            .studentCode(student.getStudentCode())
                            .studentName(student.getFullName())
                            .midtermLevel(evaluation == null ? null : evaluation.getMidtermLevel())
                            .midtermRemark(evaluation == null ? null : evaluation.getMidtermRemark())
                            .finalLevel(evaluation == null ? null : evaluation.getFinalLevel())
                            .finalRemark(evaluation == null ? null : evaluation.getFinalRemark())
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
    @Transactional
    public void bulkUpsert(EvaluationBulkUpsertRequest request) {
        Classroom classroom = findClassroom(request.getClassroomId());
        Subject subject = findSubject(request.getSubjectId());
        Semester semester = findSemester(request.getSemesterId());
        validateSemesterBelongsToClassroomSchoolYear(classroom, semester);

        for (EvaluationStudentUpsertItemDto item : request.getItems()) {
            upsertStudentEvaluation(classroom, subject, semester, item);
        }
    }

    private void upsertStudentEvaluation(Classroom classroom, Subject subject, Semester semester,
            EvaluationStudentUpsertItemDto item) {
        Student student = findStudent(item.getStudentId());
        validateStudentInClassroom(classroom.getId(), student.getId());

        String midtermLevel = normalizeLevel(item.getMidtermLevel());
        String finalLevel = normalizeLevel(item.getFinalLevel());
        String midtermRemark = normalizeNullable(item.getMidtermRemark());
        String finalRemark = normalizeNullable(item.getFinalRemark());

        StudentEvaluation evaluation = studentEvaluationRepository
                .findByClassroomIdAndSubjectIdAndSemesterIdAndStudentId(
                        classroom.getId(), subject.getId(), semester.getId(), student.getId())
                .orElse(null);

        boolean emptyPayload = !StringUtils.hasText(midtermLevel)
                && !StringUtils.hasText(finalLevel)
                && !StringUtils.hasText(midtermRemark)
                && !StringUtils.hasText(finalRemark);

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
        evaluation.setMidtermRemark(midtermRemark);
        evaluation.setFinalLevel(finalLevel);
        evaluation.setFinalRemark(finalRemark);
        studentEvaluationRepository.save(evaluation);
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

        ProgramDistribution selectedPpct = null;
        for (ProgramDistribution p : ppcts) {
            if (p.getWeekNumber() != null && p.getWeekNumber() >= weekRange[0] && p.getWeekNumber() <= weekRange[1]) {
                selectedPpct = p;
                break;
            }
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

        AiGenerateCommentRequest aiRequest = AiGenerateCommentRequest.builder()
                .gradeLevel(gradeLevelName)
                .subjectName(subjectName)
                .term(request.getTerm())
                .weekNo(weekNo)
                .lessonNo(lessonNo)
                .lessonTitle(lessonTitle)
                .learningObjective(learningObjective)
                .evaluation(request.getEvaluation())
                .attendanceStatus(attendanceStatus)
                .attendanceCode(attendanceCode)
                .participationLevel(request.getParticipationLevel())
                .behaviorTag(request.getBehaviorTag())
                .textbookSeries(textbookSeries)
                .build();

        try {
            AiGenerateCommentResponse response = restTemplate.postForObject(
                    aiGenerateCommentUrl,
                    aiRequest,
                    AiGenerateCommentResponse.class
            );
            return response != null && response.getComment() != null ? response.getComment() : "";
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserMessageException(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), "Lỗi khi gọi AI service: " + e.getMessage());
        }
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
}
