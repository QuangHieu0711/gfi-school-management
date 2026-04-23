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
