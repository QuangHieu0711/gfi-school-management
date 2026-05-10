package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.classroom.ClassroomImportResultDto;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.classroom.ClassroomCreateRequest;
import com.gfi.backend.models.dtos.classroom.ClassroomDetailDto;
import com.gfi.backend.models.dtos.classroom.ClassroomFilterDto;
import com.gfi.backend.models.dtos.classroom.ClassroomGroupItemDto;
import com.gfi.backend.models.dtos.classroom.GradeLevelClassroomGroupDto;
import com.gfi.backend.models.dtos.classroom.ClassroomListItemDto;
import com.gfi.backend.models.dtos.classroom.ClassroomUpdateRequest;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.security.FeatureKey;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.specifications.ClassroomSpecification;
import com.gfi.backend.services.interfaces.ClassroomService;
import com.gfi.backend.services.interfaces.ClassroomSubjectService;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UnitRepository unitRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomSubjectService classroomSubjectService;
    private final ClassroomSpecification classroomSpecification;
    private final DataScopeFilterService dataScopeFilterService;
    private final ImportErrorFileStorageService importErrorFileStorageService;

    private static final String FEATURE = FeatureKey.CLASS_MANAGEMENT.getCode();

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ClassroomListItemDto, ClassroomFilterDto> search(
            PageRequestDto<ClassroomFilterDto> request) {
        ClassroomFilterDto filter = request.getFilter() == null ? new ClassroomFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        Page<Classroom> classrooms = classroomRepository.findAll(
                classroomSpecification.buildSpecification(filter, resolvedScopes),
                pageable);
        Page<ClassroomListItemDto> page = classrooms.map(this::toListItemDto);

        return PageResponseDto.<ClassroomListItemDto, ClassroomFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions(Long unitId, Long gradeLevelId, Long schoolYearId) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        return classroomRepository.findAll(
                classroomSpecification.buildSpecificationForOptions(unitId, gradeLevelId, schoolYearId, resolvedScopes))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeLevelClassroomGroupDto> getGradeClassGroups(Long unitId, Long schoolYearId) {
        findUnit(unitId);
        findSchoolYear(schoolYearId);
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);

        return classroomRepository.findByUnitIdAndSchoolYearIdAndDeletedFlagOrderByGradeLevelGradeNumberAscNameAsc(
                unitId, schoolYearId, 0)
                .stream()
                .filter(classroom -> hasClassroomAccess(resolvedScopes, classroom))
                .filter(classroom -> classroom.getGradeLevel() != null)
                .collect(Collectors.groupingBy(
                        classroom -> classroom.getGradeLevel().getId(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .map(group -> {
                    GradeLevel gradeLevel = group.get(0).getGradeLevel();
                    return GradeLevelClassroomGroupDto.builder()
                            .gradeLevelId(gradeLevel.getId())
                            .gradeLevelName(gradeLevel.getName())
                            .gradeNumber(gradeLevel.getGradeNumber())
                            .classes(group.stream()
                                    .map(item -> ClassroomGroupItemDto.builder()
                                            .id(item.getId())
                                            .name(item.getName())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomDetailDto getById(Long id) {
        Classroom classroom = findClassroom(id);
        validateClassroomScope(ActionType.VIEW, classroom);
        return toDetailDto(classroom);
    }

    @Override
    @Transactional
    public ClassroomDetailDto create(ClassroomCreateRequest request) {
        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        validateClassroomTargetScope(ActionType.ADD, unit, gradeLevel);
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), code, name, null);

        Classroom classroom = new Classroom();
        classroom.setCode(code);
        classroom.setName(name);
        classroom.setUnit(unit);
        classroom.setGradeLevel(gradeLevel);
        classroom.setSchoolYear(schoolYear);
        classroom.setStatus(request.getStatus());
        classroom.setDescription(normalizeNullable(request.getDescription()));
        classroom.setCreatedBy(SecurityUtils.getCurrentUsername());
        Classroom savedClassroom = classroomRepository.save(classroom);
        classroomSubjectService.syncFromGradeLevel(savedClassroom);
        return toDetailDto(savedClassroom);
    }

    @Override
    @Transactional
    public ClassroomDetailDto update(Long id, ClassroomUpdateRequest request) {
        Classroom classroom = findClassroom(id);
        validateClassroomScope(ActionType.EDIT, classroom);

        Unit unit = findUnit(request.getUnitId());
        GradeLevel gradeLevel = findGradeLevel(request.getGradeLevelId());
        validateClassroomTargetScope(ActionType.EDIT, unit, gradeLevel);
        SchoolYear schoolYear = findSchoolYear(request.getSchoolYearId());
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        boolean gradeLevelChanged = classroom.getGradeLevel() == null
                || !classroom.getGradeLevel().getId().equals(gradeLevel.getId());

        validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), code, name, id);

        classroom.setCode(code);
        classroom.setName(name);
        classroom.setUnit(unit);
        classroom.setGradeLevel(gradeLevel);
        classroom.setSchoolYear(schoolYear);
        classroom.setStatus(request.getStatus());
        classroom.setDescription(normalizeNullable(request.getDescription()));
        classroom.setUpdatedBy(SecurityUtils.getCurrentUsername());
        Classroom savedClassroom = classroomRepository.save(classroom);
        if (gradeLevelChanged) {
            classroomSubjectService.syncFromGradeLevel(savedClassroom);
        }
        return toDetailDto(savedClassroom);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Classroom classroom = findClassroom(id);
        validateClassroomScope(ActionType.DELETE, classroom);

        classroomSubjectService.clearByClassroomId(id);
        classroom.setDeletedFlag(1);
        classroom.setDeletedAt(LocalDateTime.now());
        classroom.setDeletedBy(SecurityUtils.getCurrentUsername());
        classroomRepository.save(classroom);
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private void validateClassroomScope(ActionType action, Classroom classroom) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, action);
        if (!hasClassroomAccess(resolvedScopes, classroom)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User khong co quyen " + action + " tren classroom trong scope hien tai");
        }
    }

    private void validateClassroomTargetScope(ActionType action, Unit unit, GradeLevel gradeLevel) {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, action);
        if (!hasClassroomTargetAccess(resolvedScopes, unit, gradeLevel)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User khong co quyen " + action + " tren classroom target trong scope hien tai");
        }
    }

    private boolean hasClassroomAccess(List<ResolvedScope> resolvedScopes, Classroom classroom) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return false;
        }
        return resolvedScopes.stream().anyMatch(scope -> {
            if (scope == null) {
                return false;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                return false;
            }
            return switch (scope.getScopeType()) {
                case UNIT -> classroom.getUnit() != null && scope.getScopeIds().contains(classroom.getUnit().getId());
                case CLASS -> scope.getScopeIds().contains(classroom.getId());
                case GRADE -> classroom.getGradeLevel() != null
                        && scope.getScopeIds().contains(classroom.getGradeLevel().getId());
                default -> false;
            };
        });
    }

    private boolean hasClassroomTargetAccess(List<ResolvedScope> resolvedScopes, Unit unit, GradeLevel gradeLevel) {
        if (resolvedScopes == null || resolvedScopes.isEmpty()) {
            return false;
        }
        return resolvedScopes.stream().anyMatch(scope -> {
            if (scope == null) {
                return false;
            }
            if (scope.isUnrestricted() || scope.getScopeType() == ScopeType.ALL) {
                return true;
            }
            if (scope.getScopeIds() == null || scope.getScopeIds().isEmpty()) {
                return false;
            }
            return switch (scope.getScopeType()) {
                case UNIT -> unit != null && scope.getScopeIds().contains(unit.getId());
                case GRADE -> gradeLevel != null && scope.getScopeIds().contains(gradeLevel.getId());
                case CLASS -> false;
                default -> false;
            };
        });
    }

    private Unit findUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private void validateUnique(Long unitId, Long gradeLevelId, Long schoolYearId, String code, String name, Long id) {
        classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearIdAndCode(unitId, gradeLevelId, schoolYearId, code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.CLASS_CODE_ALREADY_EXISTS);
                });
        classroomRepository.findByUnitIdAndGradeLevelIdAndSchoolYearIdAndName(unitId, gradeLevelId, schoolYearId, name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.CLASS_NAME_ALREADY_EXISTS);
                });
    }

    private ClassroomDetailDto toDetailDto(Classroom classroom) {
        return ClassroomDetailDto.builder()
                .id(classroom.getId())
                .code(classroom.getCode())
                .name(classroom.getName())
                .unitId(classroom.getUnit() == null ? null : classroom.getUnit().getId())
                .gradeLevelId(classroom.getGradeLevel() == null ? null : classroom.getGradeLevel().getId())
                .schoolYearId(classroom.getSchoolYear() == null ? null : classroom.getSchoolYear().getId())
                .status(classroom.getStatus())
                .description(classroom.getDescription())
                .build();
    }

    private ClassroomListItemDto toListItemDto(Classroom classroom) {
        return ClassroomListItemDto.builder()
                .id(classroom.getId())
                .code(classroom.getCode())
                .name(classroom.getName())
                .unitName(classroom.getUnit() == null ? null : classroom.getUnit().getName())
                .gradeLevelName(classroom.getGradeLevel() == null ? null : classroom.getGradeLevel().getName())
                .schoolYearName(classroom.getSchoolYear() == null ? null : classroom.getSchoolYear().getName())
                .status(classroom.getStatus())
                .build();
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private int normalizePageNow(Integer pageNow) {
        if (pageNow == null || pageNow < 0) {
            return 0;
        }
        return pageNow;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional(readOnly = true)
    public byte[] export(PageRequestDto<ClassroomFilterDto> request, ExportType exportType) {
        ClassroomFilterDto filter = request.getFilter() == null ? new ClassroomFilterDto() : request.getFilter();
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        List<Classroom> items = classroomRepository.findAll(
                classroomSpecification.buildSpecification(filter, resolvedScopes),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name").and(org.springframework.data.domain.Sort.by("id")));

        if (exportType == ExportType.PDF) {
            return exportClassroomsPdf(items);
        }
        return exportClassroomsExcel(items);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate() {
        return exportClassroomsExcelTemplate();
    }

    @Override
    @Transactional
    public ClassroomImportResultDto importExcel(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new UserMessageException("File import phải là file Excel .xlsx");
        }
        
        int successCount = 0;
        int failedCount = 0;
        Map<Integer, String> rowErrors = new LinkedHashMap<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("LopHoc");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new UserMessageException("File Excel không có sheet LopHoc");
            }

            DataFormatter formatter = new DataFormatter();
            int dataStartRowIndex = findClassroomImportDataStartRow(sheet, formatter);

            for (int rowIndex = dataStartRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                
                boolean isEmpty = true;
                for (int c = 1; c <= 7; c++) {
                    if (StringUtils.hasText(readCellText(row.getCell(c), formatter))) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) continue;

                String code = readCellText(row.getCell(1), formatter);
                String name = readCellText(row.getCell(2), formatter);
                String unitCode = readCellText(row.getCell(3), formatter);
                String gradeCode = readCellText(row.getCell(4), formatter);
                String schoolYearName = readCellText(row.getCell(5), formatter);
                String statusText = readCellText(row.getCell(6), formatter);
                String description = readCellText(row.getCell(7), formatter);

                if (!StringUtils.hasText(code) || !StringUtils.hasText(name) || !StringUtils.hasText(unitCode) || !StringUtils.hasText(gradeCode) || !StringUtils.hasText(schoolYearName)) {
                    failedCount++;
                    rowErrors.put(rowIndex, "Mã lớp, tên lớp, mã đơn vị, mã khối, tên năm học là bắt buộc");
                    continue;
                }

                String normalizedCode = code.trim();
                
                try {
                    Unit unit = unitRepository.findByCode(unitCode.trim()).orElseThrow(() -> new UserMessageException("Đơn vị không tồn tại"));
                    GradeLevel gradeLevel = gradeLevelRepository.findByCode(gradeCode.trim()).orElseThrow(() -> new UserMessageException("Khối không tồn tại"));
                    SchoolYear schoolYear = schoolYearRepository.findByName(schoolYearName.trim()).orElseThrow(() -> new UserMessageException("Năm học không tồn tại"));
                    
                    validateClassroomTargetScope(ActionType.ADD, unit, gradeLevel);
                    validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), normalizedCode, name.trim(), null);

                    Classroom classroom = new Classroom();
                    classroom.setCode(normalizedCode);
                    classroom.setName(name.trim());
                    classroom.setUnit(unit);
                    classroom.setGradeLevel(gradeLevel);
                    classroom.setSchoolYear(schoolYear);
                    classroom.setDescription(normalizeNullable(description));
                    
                    String normStatus = normalizeImportText(statusText);
                    Integer status = 1;
                    if ("0".equals(normStatus) || "khong hoat dong".equals(normStatus)) {
                        status = 0;
                    }
                    classroom.setStatus(status);
                    classroom.setDeletedFlag(0);
                    classroom.setCreatedBy(SecurityUtils.getCurrentUsername());
                    Classroom saved = classroomRepository.save(classroom);
                    classroomSubjectService.syncFromGradeLevel(saved);
                    successCount++;
                } catch (Exception ex) {
                    failedCount++;
                    String msg = ex instanceof UserMessageException ? ex.getMessage() : "Dữ liệu không hợp lệ";
                    rowErrors.put(rowIndex, msg);
                }
            }

            String errorFileToken = null;
            String errorFileName = null;
            if (!rowErrors.isEmpty()) {
                byte[] errorFileContent = buildClassroomImportErrorFile(workbook, sheet, rowErrors);
                errorFileName = "classroom_import_error_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
                errorFileToken = importErrorFileStorageService.store(
                        errorFileName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        errorFileContent);
            }

            return ClassroomImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .hasErrorFile(!rowErrors.isEmpty())
                    .errorFileName(errorFileName)
                    .errorFileToken(errorFileToken)
                    .build();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file Excel lớp học");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }
    
    private String readCellText(org.apache.poi.ss.usermodel.Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
    
    private String normalizeImportText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace('đ', 'd');
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private int findClassroomImportDataStartRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String codeHeader = normalizeImportText(readCellText(row.getCell(1), formatter));
            String nameHeader = normalizeImportText(readCellText(row.getCell(2), formatter));
            if ("ma lop".equals(codeHeader) && "ten lop".equals(nameHeader)) {
                return rowIndex + 1;
            }
        }
        throw new UserMessageException("File import không đúng định dạng: không tìm thấy dòng tiêu đề dữ liệu");
    }

    private byte[] buildClassroomImportErrorFile(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors) {
        CellStyle resultHeaderStyle = workbook.createCellStyle();
        resultHeaderStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        resultHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        resultHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        resultHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        resultHeaderStyle.setBorderTop(BorderStyle.THIN);
        resultHeaderStyle.setBorderBottom(BorderStyle.THIN);
        resultHeaderStyle.setBorderLeft(BorderStyle.THIN);
        resultHeaderStyle.setBorderRight(BorderStyle.THIN);
        org.apache.poi.ss.usermodel.Font f = workbook.createFont();
        f.setBold(true);
        f.setFontName(EXPORT_FONT_NAME);
        resultHeaderStyle.setFont(f);

        CellStyle errorCellStyle = workbook.createCellStyle();
        errorCellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        errorCellStyle.setBorderTop(BorderStyle.THIN);
        errorCellStyle.setBorderBottom(BorderStyle.THIN);
        errorCellStyle.setBorderLeft(BorderStyle.THIN);
        errorCellStyle.setBorderRight(BorderStyle.THIN);
        errorCellStyle.setWrapText(true);
        org.apache.poi.ss.usermodel.Font ef = workbook.createFont();
        ef.setFontName(EXPORT_FONT_NAME);
        ef.setColor(IndexedColors.RED.getIndex());
        errorCellStyle.setFont(ef);

        int resultColumnIndex = 8;
        int reasonColumnIndex = 9;

        int headerRowIndex = findClassroomImportDataStartRow(sheet, new DataFormatter()) - 1;
        org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) headerRow = sheet.createRow(headerRowIndex);
        org.apache.poi.ss.usermodel.Cell c1 = headerRow.createCell(resultColumnIndex); c1.setCellValue("Kết quả"); c1.setCellStyle(resultHeaderStyle);
        org.apache.poi.ss.usermodel.Cell c2 = headerRow.createCell(reasonColumnIndex); c2.setCellValue("Lý do lỗi"); c2.setCellStyle(resultHeaderStyle);

        for (Map.Entry<Integer, String> entry : rowErrors.entrySet()) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(entry.getKey());
            if (row == null) continue;
            for (int columnIndex = 0; columnIndex <= reasonColumnIndex; columnIndex++) {
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                    cell.setCellStyle(errorCellStyle);
                } else {
                    CellStyle style = workbook.createCellStyle();
                    if (cell.getCellStyle() != null) style.cloneStyleFrom(cell.getCellStyle());
                    org.apache.poi.ss.usermodel.Font ff = workbook.createFont();
                    ff.setFontName(EXPORT_FONT_NAME);
                    ff.setColor(IndexedColors.RED.getIndex());
                    style.setFont(ff);
                    style.setFillPattern(FillPatternType.NO_FILL);
                    cell.setCellStyle(style);
                }
            }
            row.getCell(resultColumnIndex).setCellValue("Thất bại");
            row.getCell(reasonColumnIndex).setCellValue(entry.getValue());
        }

        sheet.setColumnWidth(resultColumnIndex, 16 * 256);
        sheet.setColumnWidth(reasonColumnIndex, 42 * 256);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file lỗi import");
        }
    }

    private byte[] exportClassroomsExcel(List<Classroom> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("LopHoc");
            
            org.apache.poi.ss.usermodel.Row infoRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("Thời gian tải: " + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + " | Người tải: " + SecurityUtils.getCurrentUsername());
            CellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setAlignment(HorizontalAlignment.RIGHT);
            infoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font infoFont = workbook.createFont();
            infoFont.setFontName(EXPORT_FONT_NAME);
            infoStyle.setFont(infoFont);
            infoCell.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH LỚP HỌC");
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setFontName(EXPORT_FONT_NAME);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(3);
            String[] headers = { "STT", "Mã lớp", "Tên lớp", "Đơn vị", "Khối", "Năm học", "Mô tả", "Trạng thái" };
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor((short) 41);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName(EXPORT_FONT_NAME);
            headerStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setVerticalAlignment(VerticalAlignment.TOP);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            bodyStyle.setWrapText(true);
            org.apache.poi.ss.usermodel.Font bodyFont = workbook.createFont();
            bodyFont.setFontName(EXPORT_FONT_NAME);
            bodyStyle.setFont(bodyFont);

            int rowIndex = 4;
            int stt = 1;
            for (Classroom item : items) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex++);
                int col = 0;
                org.apache.poi.ss.usermodel.Cell c = row.createCell(col++); c.setCellValue(stt++); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getCode()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getName()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getUnit() != null ? item.getUnit().getName() : ""); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getGradeLevel() != null ? item.getGradeLevel().getName() : ""); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getSchoolYear() != null ? item.getSchoolYear().getName() : ""); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getDescription()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(Integer.valueOf(1).equals(item.getStatus()) ? "Hoạt động" : "Không hoạt động"); c.setCellStyle(bodyStyle);
            }
            
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 4000); // Tăng padding tối đa
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel");
        }
    }

    private byte[] exportClassroomsExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("LopHoc");
            
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("MẪU IMPORT DANH SÁCH LỚP HỌC");
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setFontName(EXPORT_FONT_NAME);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            CellStyle guideStyle = workbook.createCellStyle();
            guideStyle.setAlignment(HorizontalAlignment.LEFT);
            guideStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            guideStyle.setWrapText(true);
            org.apache.poi.ss.usermodel.Font guideFont = workbook.createFont();
            guideFont.setItalic(true);
            guideFont.setFontName(EXPORT_FONT_NAME);
            guideFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            guideStyle.setFont(guideFont);
            
            org.apache.poi.ss.usermodel.Row guide1 = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell gc1 = guide1.createCell(0); 
            gc1.setCellValue("Hướng dẫn: Điền mã lớp, tên lớp, mã đơn vị, mã khối, tên năm học. \nTrạng thái: Hoạt động / Không hoạt động. Cột (*) là bắt buộc."); 
            gc1.setCellStyle(guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
            
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(3);
            String[] headers = { "STT", "Mã lớp (*)", "Tên lớp (*)", "Mã đơn vị (*)", "Mã khối (*)", "Tên năm học (*)", "Trạng thái", "Mô tả" };
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE1.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName(EXPORT_FONT_NAME);
            headerStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            bodyStyle.setWrapText(true);
            org.apache.poi.ss.usermodel.Font bodyFont = workbook.createFont();
            bodyFont.setFontName(EXPORT_FONT_NAME);
            bodyStyle.setFont(bodyFont);

            org.apache.poi.ss.usermodel.Row sampleRow = sheet.createRow(4);
            int col = 0;
            org.apache.poi.ss.usermodel.Cell c = sampleRow.createCell(col++); c.setCellValue("1"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("THVN050_5B_1"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("Lớp 5B"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("THVN050"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("KHOI_5"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("Năm học 2025 - 2026"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("Hoạt động"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue("Ghi chú lớp học mẫu"); c.setCellStyle(bodyStyle);
            
            // Set maximum explicit widths for premium spacing
            sheet.setColumnWidth(0, 10 * 256);  // STT
            sheet.setColumnWidth(1, 40 * 256);  // Mã lớp
            sheet.setColumnWidth(2, 45 * 256);  // Tên lớp
            sheet.setColumnWidth(3, 30 * 256);  // Mã đơn vị
            sheet.setColumnWidth(4, 25 * 256);  // Mã khối
            sheet.setColumnWidth(5, 55 * 256);  // Tên năm học
            sheet.setColumnWidth(6, 25 * 256);  // Trạng thái
            sheet.setColumnWidth(7, 65 * 256);  // Mô tả
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file template");
        }
    }

    private byte[] exportClassroomsPdf(List<Classroom> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 20, 20);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font infoFont = createPdfFont(10, com.lowagie.text.Font.ITALIC);

            Paragraph exportInfo = new Paragraph("Thời gian tải: " + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + " | Người tải: " + SecurityUtils.getCurrentUsername(), infoFont);
            exportInfo.setAlignment(Element.ALIGN_RIGHT);
            exportInfo.setSpacingAfter(6f);
            document.add(exportInfo);

            Paragraph title = new Paragraph("DANH SÁCH LỚP HỌC", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.8f, 1.8f, 2.6f, 3.0f, 1.5f, 2.5f, 1.5f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Mã lớp", headerFont);
            addPdfHeaderCell(table, "Tên lớp", headerFont);
            addPdfHeaderCell(table, "Đơn vị", headerFont);
            addPdfHeaderCell(table, "Khối", headerFont);
            addPdfHeaderCell(table, "Năm học", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);

            int stt = 1;
            for (Classroom item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getUnit() != null ? item.getUnit().getName() : "", bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getGradeLevel() != null ? item.getGradeLevel().getName() : "", bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getSchoolYear() != null ? item.getSchoolYear().getName() : "", bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, Integer.valueOf(1).equals(item.getStatus()) ? "Hoạt động" : "Không hoạt động", bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF");
        }
    }

    private void addPdfHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new java.awt.Color(224, 242, 241));
        table.addCell(cell);
    }

    private void addPdfBodyCell(PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private com.lowagie.text.Font createPdfFont(float size, int style) {
        String fontPath = switch (style) {
            case com.lowagie.text.Font.BOLD -> TIMES_FONT_BOLD_PATH;
            case com.lowagie.text.Font.ITALIC -> TIMES_FONT_ITALIC_PATH;
            default -> TIMES_FONT_REGULAR_PATH;
        };
        try {
            if (Files.exists(Path.of(fontPath))) {
                BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new com.lowagie.text.Font(baseFont, size, com.lowagie.text.Font.NORMAL);
            }
        } catch (Exception ignored) {}
        return com.lowagie.text.FontFactory.getFont(EXPORT_FONT_NAME, BaseFont.IDENTITY_H, true, size, style);
    }
}
