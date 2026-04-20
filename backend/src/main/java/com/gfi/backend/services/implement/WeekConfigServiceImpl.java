package com.gfi.backend.services.implement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigBulkUpdateItemRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigBulkUpdateRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigCbbDto;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigGenerateRequest;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigItemDto;
import com.gfi.backend.models.dtos.weekconfig.WeekConfigUpdateRequest;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Semester;
import com.gfi.backend.models.entities.WeekConfig;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.repositories.WeekConfigRepository;
import com.gfi.backend.services.interfaces.WeekConfigService;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeekConfigServiceImpl implements WeekConfigService {

    private final WeekConfigRepository weekConfigRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WeekConfigItemDto> getWeekConfigs(Long schoolYearId, Long semesterId) {
        SchoolYear schoolYear = findSchoolYear(schoolYearId);
        if (semesterId != null) {
            Semester semester = findSemester(semesterId);
            validateSemesterBelongsToSchoolYear(semester, schoolYear.getId());
        }

        return weekConfigRepository.search(schoolYear.getId(), semesterId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<WeekConfigItemDto> generate(WeekConfigGenerateRequest request) {
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        Semester semester = findSemester(request.getSemesterId());
        validateSemesterBelongsToSchoolYear(semester, schoolYear.getId());

        // Validation: nếu không phải sinh lại, phải kiểm tra điều kiện
        if (!Boolean.TRUE.equals(request.getForceRegenerate())) {
            if (weekConfigRepository.existsBySemesterIdAndDeletedFlag(semester.getId(), 0)) {
                throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_ALREADY_EXISTS_FOR_SEMESTER);
            }
            
            // Kiểm tra: nếu không phải HK1, phải có HK trước đã được sinh
            List<Semester> semesters = semesterRepository.findBySchoolYearId(schoolYear.getId()).stream()
                    .filter(s -> s.getDeletedFlag() == 0)
                    .sorted(Comparator.comparing(Semester::getSemesterOrder))
                    .toList();
            
            int currentIndex = semesters.indexOf(semester);
            if (currentIndex > 0) {
                // Có HK trước, phải kiểm tra HK trước đã sinh tuần chưa
                Semester previousSemester = semesters.get(currentIndex - 1);
                boolean previousHasWeeks = weekConfigRepository.existsBySemesterIdAndDeletedFlag(previousSemester.getId(), 0);
                if (!previousHasWeeks) {
                    throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_PREVIOUS_SEMESTER_NOT_GENERATED);
                }
            }
        }

        // Nếu forceRegenerate = true, soft delete các tuần cũ
        if (Boolean.TRUE.equals(request.getForceRegenerate())) {
            deleteBySemester(semester.getId());
        }

        List<WeekConfig> generated = new ArrayList<>();
        List<WeekConfig> reusableDeletedWeeks = new ArrayList<>(
                weekConfigRepository.findBySemesterIdAndDeletedFlagOrderByIdAsc(semester.getId(), 1));
        LocalDate cursor = semester.getStartDate();
        
        // Tính toán số tuần bắt đầu: lấy tuần cuối cùng từ các học kỳ trước + 1
        int weekNumber = calculateStartWeekNumber(schoolYear, semester);

        while (!cursor.isAfter(semester.getEndDate())) {
            LocalDate endDate = cursor.plusDays(6);
            if (endDate.isAfter(semester.getEndDate())) {
                endDate = semester.getEndDate();
            }

            WeekConfig item = takeReusableWeekConfig(reusableDeletedWeeks);
            if (item == null) {
                item = new WeekConfig();
                item.setSchoolYear(schoolYear);
                item.setSemester(semester);
                item.setCreatedBy(getCurrentUsername());
            } else {
                reactivateWeekConfig(item);
            }
            item.setWeekNumber(weekNumber);
            item.setStartDate(cursor);
            item.setEndDate(endDate);
            item.setUpdatedBy(getCurrentUsername());
            generated.add(item);

            cursor = endDate.plusDays(1);
            weekNumber++;
        }

        return weekConfigRepository.saveAll(generated).stream().map(this::toDto).toList();
    }

    private int calculateStartWeekNumber(SchoolYear schoolYear, Semester currentSemester) {
        // Lấy tất cả học kỳ trong năm học (theo thứ tự), chỉ lấy những cái chưa bị xóa
        List<Semester> semesters = semesterRepository.findBySchoolYearId(schoolYear.getId()).stream()
                .filter(s -> s.getDeletedFlag() == 0)
                .sorted(Comparator.comparing(Semester::getSemesterOrder))
                .toList();
        
        int currentSemesterIndex = -1;
        for (int i = 0; i < semesters.size(); i++) {
            if (semesters.get(i).getId().equals(currentSemester.getId())) {
                currentSemesterIndex = i;
                break;
            }
        }

        // Nếu là học kỳ đầu tiên, bắt đầu từ tuần 1
        if (currentSemesterIndex <= 0) {
            return 1;
        }

        // Nếu không, tìm tuần cuối cùng của học kỳ trước (chỉ lấy những tuần chưa bị xóa)
        Semester previousSemester = semesters.get(currentSemesterIndex - 1);
        List<WeekConfig> previousWeeks = weekConfigRepository
                .findBySemesterIdAndDeletedFlagOrderByWeekNumberDescIdAsc(previousSemester.getId(), 0);
        
        if (previousWeeks.isEmpty()) {
            return 1;
        }

        // Lấy số tuần lớn nhất từ học kỳ trước + 1
        int maxWeekNumber = previousWeeks.get(0).getWeekNumber();
        return maxWeekNumber + 1;
    }

    @Override
    @Transactional
    public WeekConfigItemDto update(Long id, WeekConfigUpdateRequest request) {
        WeekConfig weekConfig = findWeekConfig(id);

        validateDateRange(request.getStartDate(), request.getEndDate());
        validateWeekInsideSemester(weekConfig.getSemester(), request.getStartDate(), request.getEndDate());
        validateEditedWeekNumber(weekConfig, request.getWeekNumber());

        cascadeUpdateFromWeek(weekConfig, request.getStartDate(), request.getEndDate());

        return toDto(findWeekConfig(id));
    }

    @Override
    @Transactional
    public List<WeekConfigItemDto> bulkUpdate(WeekConfigBulkUpdateRequest request) {
        List<WeekConfigBulkUpdateItemRequest> items = request.getItems();

        Set<Long> uniqueIds = new HashSet<>();
        for (WeekConfigBulkUpdateItemRequest item : items) {
            if (!uniqueIds.add(item.getId())) {
                throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_BULK_DUPLICATE_ID);
            }
            validateDateRange(item.getStartDate(), item.getEndDate());
        }

        List<WeekConfig> weekConfigs = weekConfigRepository.findAllById(uniqueIds);
        if (weekConfigs.size() != uniqueIds.size()) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_NOT_FOUND);
        }

        Map<Long, WeekConfig> weekConfigById = weekConfigs.stream()
                .filter(w -> w.getDeletedFlag() == 0)
                .collect(Collectors.toMap(WeekConfig::getId, w -> w));

        if (weekConfigById.size() != uniqueIds.size()) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_NOT_FOUND);
        }

        Map<Long, WeekConfigBulkUpdateItemRequest> requestById = items.stream()
                .collect(Collectors.toMap(WeekConfigBulkUpdateItemRequest::getId, i -> i));

        Map<Long, List<WeekConfig>> weekConfigsBySemester = weekConfigs.stream()
                .collect(Collectors.groupingBy(w -> w.getSemester().getId()));

        Set<Long> touchedSemesterIds = new HashSet<>();

        for (Map.Entry<Long, List<WeekConfig>> entry : weekConfigsBySemester.entrySet()) {
            List<WeekConfig> semesterWeeks = entry.getValue().stream()
                    .sorted(Comparator.comparing(WeekConfig::getWeekNumber).thenComparing(WeekConfig::getId))
                    .toList();

            WeekConfig anchorWeek = null;
            WeekConfigBulkUpdateItemRequest anchorRequest = null;

            for (WeekConfig weekConfig : semesterWeeks) {
                WeekConfigBulkUpdateItemRequest item = requestById.get(weekConfig.getId());
                validateEditedWeekNumber(weekConfig, item.getWeekNumber());
                validateWeekInsideSemester(weekConfig.getSemester(), item.getStartDate(), item.getEndDate());

                if (anchorWeek == null && isDateChanged(weekConfig, item)) {
                    anchorWeek = weekConfig;
                    anchorRequest = item;
                }
            }

            if (anchorWeek != null) {
                cascadeUpdateFromWeek(anchorWeek, anchorRequest.getStartDate(), anchorRequest.getEndDate());
                touchedSemesterIds.add(anchorWeek.getSemester().getId());
            }
        }

        if (touchedSemesterIds.isEmpty()) {
            for (WeekConfig weekConfig : weekConfigs) {
                WeekConfigBulkUpdateItemRequest item = requestById.get(weekConfig.getId());
                validateEditedWeekNumber(weekConfig, item.getWeekNumber());
                validateWeekInsideSemester(weekConfig.getSemester(), item.getStartDate(), item.getEndDate());
                weekConfig.setStartDate(item.getStartDate());
                weekConfig.setEndDate(item.getEndDate());
                weekConfig.setUpdatedBy(getCurrentUsername());
            }
            weekConfigRepository.saveAll(weekConfigs);
            touchedSemesterIds.addAll(weekConfigsBySemester.keySet());
        }

        return touchedSemesterIds.stream()
                .flatMap(semesterId -> weekConfigRepository
                        .findBySemesterIdAndDeletedFlagOrderByWeekNumberAscIdAsc(semesterId, 0).stream())
                .sorted(Comparator.comparing((WeekConfig w) -> w.getSemester().getSemesterOrder())
                        .thenComparing(WeekConfig::getWeekNumber)
                        .thenComparing(WeekConfig::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteBySemester(Long semesterId) {
        Semester semester = findSemester(semesterId);
        List<WeekConfig> weekConfigs = weekConfigRepository
                .findBySemesterIdAndDeletedFlagOrderByWeekNumberAscIdAsc(semester.getId(), 0);

        for (WeekConfig weekConfig : weekConfigs) {
            weekConfig.setDeletedFlag(1);
            weekConfig.setDeletedAt(LocalDateTime.now());
            weekConfig.setDeletedBy(getCurrentUsername());
        }

        weekConfigRepository.saveAll(weekConfigs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeekConfigCbbDto> getWeekConfigsCbb() {
        List<WeekConfig> weekConfigs = weekConfigRepository
                .findByDeletedFlagOrderBySemesterSemesterOrderAscWeekNumberAscIdAsc(0);
        
        return weekConfigs.stream()
                .map(w -> WeekConfigCbbDto.builder()
                        .id(w.getId())
                        .name("Tuần " + w.getWeekNumber())
                        .build())
                .toList();
    }

    private void validateBulkData(List<WeekConfig> updatingTargets,
            Map<Long, WeekConfigBulkUpdateItemRequest> requestById) {
        Set<Long> semesterIds = updatingTargets.stream().map(w -> w.getSemester().getId()).collect(Collectors.toSet());

        for (Long semesterId : semesterIds) {
            List<WeekConfig> allConfigsInSemester = weekConfigRepository
                    .findBySemesterIdAndDeletedFlagOrderByWeekNumberAscIdAsc(semesterId, 0);

            Map<Long, CandidateWeekData> candidates = new HashMap<>();
            for (WeekConfig existing : allConfigsInSemester) {
                WeekConfigBulkUpdateItemRequest updateReq = requestById.get(existing.getId());
                if (updateReq != null) {
                    candidates.put(existing.getId(), new CandidateWeekData(existing.getId(), updateReq.getWeekNumber(),
                            updateReq.getStartDate(), updateReq.getEndDate()));
                } else {
                    candidates.put(existing.getId(), new CandidateWeekData(existing.getId(), existing.getWeekNumber(),
                            existing.getStartDate(), existing.getEndDate()));
                }
            }

            Set<Integer> uniqueWeekNumbers = new HashSet<>();
            for (CandidateWeekData data : candidates.values()) {
                if (!uniqueWeekNumbers.add(data.weekNumber())) {
                    throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_WEEK_NUMBER_ALREADY_EXISTS);
                }
            }

            List<CandidateWeekData> sortedByStartDate = candidates.values().stream()
                    .sorted(Comparator.comparing(CandidateWeekData::startDate)
                            .thenComparing(CandidateWeekData::endDate)
                            .thenComparing(CandidateWeekData::id))
                    .toList();

            for (int i = 1; i < sortedByStartDate.size(); i++) {
                CandidateWeekData previous = sortedByStartDate.get(i - 1);
                CandidateWeekData current = sortedByStartDate.get(i);
                if (!current.startDate().isAfter(previous.endDate())) {
                    throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_DATE_OVERLAP);
                }
            }
        }
    }

    private void validateWeekNumberUnique(Long semesterId, Integer weekNumber, Long excludeId) {
        weekConfigRepository.findBySemesterIdAndWeekNumberAndDeletedFlag(semesterId, weekNumber, 0)
                .filter(item -> excludeId == null || !item.getId().equals(excludeId))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_WEEK_NUMBER_ALREADY_EXISTS);
                });
    }

    private void validateEditedWeekNumber(WeekConfig currentWeekConfig, Integer requestWeekNumber) {
        if (!currentWeekConfig.getWeekNumber().equals(requestWeekNumber)) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_WEEK_NUMBER_ALREADY_EXISTS);
        }
    }

    private boolean isDateChanged(WeekConfig currentWeekConfig, WeekConfigBulkUpdateItemRequest requestItem) {
        return !currentWeekConfig.getStartDate().equals(requestItem.getStartDate())
                || !currentWeekConfig.getEndDate().equals(requestItem.getEndDate());
    }

    private void validateNoOverlappingWeek(Long semesterId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<WeekConfig> existing = weekConfigRepository.findBySemesterIdAndDeletedFlagOrderByWeekNumberAscIdAsc(semesterId, 0);

        for (WeekConfig item : existing) {
            if (excludeId != null && item.getId().equals(excludeId)) {
                continue;
            }
            if (!startDate.isAfter(item.getEndDate()) && !endDate.isBefore(item.getStartDate())) {
                throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_DATE_OVERLAP);
            }
        }
    }

    private void validateWeekInsideSemester(Semester semester, LocalDate startDate, LocalDate endDate) {
        if (startDate.isBefore(semester.getStartDate())) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_START_DATE_INVALID);
        }
        if (endDate.isAfter(semester.getEndDate())) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_END_DATE_INVALID);
        }
    }

    private void cascadeUpdateFromWeek(WeekConfig anchorWeek, LocalDate updatedStartDate, LocalDate updatedEndDate) {
        Semester semester = anchorWeek.getSemester();
        List<WeekConfig> existingWeeks = weekConfigRepository
                .findBySemesterIdAndDeletedFlagOrderByWeekNumberAscIdAsc(semester.getId(), 0);
        List<WeekConfig> reusableDeletedWeeks = new ArrayList<>(
                weekConfigRepository.findBySemesterIdAndDeletedFlagOrderByIdAsc(semester.getId(), 1));

        List<WeekConfig> affectedWeeks = existingWeeks.stream()
                .filter(item -> item.getWeekNumber() >= anchorWeek.getWeekNumber())
                .sorted(Comparator.comparing(WeekConfig::getWeekNumber).thenComparing(WeekConfig::getId))
                .toList();

        LocalDate nextStartDate = updatedStartDate;
        int nextWeekNumber = anchorWeek.getWeekNumber();
        int affectedIndex = 0;
        List<WeekConfig> toSave = new ArrayList<>();

        while (!nextStartDate.isAfter(semester.getEndDate())) {
            LocalDate nextEndDate = nextWeekNumber == anchorWeek.getWeekNumber()
                    ? updatedEndDate
                    : nextStartDate.plusDays(6);

            if (nextEndDate.isAfter(semester.getEndDate())) {
                nextEndDate = semester.getEndDate();
            }

            WeekConfig targetWeek;
            if (affectedIndex < affectedWeeks.size()) {
                targetWeek = affectedWeeks.get(affectedIndex++);
            } else {
                targetWeek = takeReusableWeekConfig(reusableDeletedWeeks);
                if (targetWeek == null) {
                    targetWeek = new WeekConfig();
                    targetWeek.setSchoolYear(anchorWeek.getSchoolYear());
                    targetWeek.setSemester(semester);
                    targetWeek.setCreatedBy(getCurrentUsername());
                } else {
                    reactivateWeekConfig(targetWeek);
                }
            }

            targetWeek.setWeekNumber(nextWeekNumber);
            targetWeek.setStartDate(nextStartDate);
            targetWeek.setEndDate(nextEndDate);
            targetWeek.setUpdatedBy(getCurrentUsername());
            toSave.add(targetWeek);

            nextStartDate = nextEndDate.plusDays(1);
            nextWeekNumber++;
        }

        List<WeekConfig> redundantWeeks = affectedWeeks.subList(affectedIndex, affectedWeeks.size());
        for (WeekConfig redundantWeek : redundantWeeks) {
            redundantWeek.setDeletedFlag(1);
            redundantWeek.setDeletedAt(LocalDateTime.now());
            redundantWeek.setDeletedBy(getCurrentUsername());
            redundantWeek.setUpdatedBy(getCurrentUsername());
        }

        weekConfigRepository.saveAll(toSave);
        if (!redundantWeeks.isEmpty()) {
            weekConfigRepository.saveAll(redundantWeeks);
        }
    }

    private void validateSemesterBelongsToSchoolYear(Semester semester, Long schoolYearId) {
        if (!semester.getSchoolYear().getId().equals(schoolYearId)) {
            throw new UserMessageException(CommonErrorCode.WEEK_CONFIG_SEMESTER_SCHOOL_YEAR_MISMATCH);
        }
    }

    private WeekConfig takeReusableWeekConfig(List<WeekConfig> reusableDeletedWeeks) {
        if (reusableDeletedWeeks.isEmpty()) {
            return null;
        }
        return reusableDeletedWeeks.remove(0);
    }

    private void reactivateWeekConfig(WeekConfig weekConfig) {
        weekConfig.setDeletedFlag(0);
        weekConfig.setDeletedAt(null);
        weekConfig.setDeletedBy(null);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new UserMessageException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .filter(schoolYear -> schoolYear.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private Semester findSemester(Long id) {
        return semesterRepository.findById(id)
                .filter(semester -> semester.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SEMESTER_NOT_FOUND));
    }

    private WeekConfig findWeekConfig(Long id) {
        return weekConfigRepository.findByIdAndDeletedFlag(id, 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.WEEK_CONFIG_NOT_FOUND));
    }

    private WeekConfigItemDto toDto(WeekConfig item) {
        return WeekConfigItemDto.builder()
                .id(item.getId())
                .schoolYearId(item.getSchoolYear() == null ? null : item.getSchoolYear().getId())
                .schoolYearName(item.getSchoolYear() == null ? null : item.getSchoolYear().getName())
                .semesterId(item.getSemester() == null ? null : item.getSemester().getId())
                .semesterName(item.getSemester() == null ? null : item.getSemester().getName())
                .weekNumber(item.getWeekNumber())
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .build();
    }

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    private record CandidateWeekData(Long id, Integer weekNumber, LocalDate startDate, LocalDate endDate) {
    }
}
