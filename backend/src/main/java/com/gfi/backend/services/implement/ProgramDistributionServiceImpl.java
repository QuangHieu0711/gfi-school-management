package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionDetailDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionFilterDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionItemDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionImportResultDto;
import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionUpdateRequest;import com.gfi.backend.models.dtos.programdistribution.ProgramDistributionCreateRequest;import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.ProgramDistribution;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.entities.WeekConfig;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.ClassroomSubjectRepository;
import com.gfi.backend.repositories.ProgramDistributionRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.WeekConfigRepository;
import com.gfi.backend.repositories.specifications.ProgramDistributionSpecification;
import com.gfi.backend.services.interfaces.ProgramDistributionService;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.IndexedColors;

@Service
@RequiredArgsConstructor
public class ProgramDistributionServiceImpl implements ProgramDistributionService {

    private static final int HEADER_ROW_INDEX = 5;
    private static final int DATA_START_ROW_INDEX = 6;

    private final ProgramDistributionRepository programDistributionRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final WeekConfigRepository weekConfigRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;
    private final ProgramDistributionSpecification programDistributionSpecification;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate(Long schoolYearId, Long unitId, Long classroomId, Long subjectId) {
        ExportContext context = buildContext(schoolYearId, unitId, classroomId, subjectId);
        List<ProgramDistribution> existingItems = programDistributionRepository
                .findBySchoolYearIdAndUnitIdAndClassroomIdAndSubjectIdAndDeletedFlagOrderByOrderNumberAscIdAsc(
                        schoolYearId, unitId, classroomId, subjectId, 0);

        List<TemplateRowData> rows = buildTemplateRows(context.weekConfigs(), existingItems);
        if (rows.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_TEMPLATE_DATA_NOT_FOUND);
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("PPCT");
            buildTemplateSheet(workbook, sheet, context, rows);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }
    }

    @Override
    @Transactional
    public ProgramDistributionImportResultDto importExcel(Long schoolYearId, Long unitId, Long classroomId,
            Long subjectId,
            MultipartFile file) {
        validateExcelFile(file);

        ExportContext context = buildContext(schoolYearId, unitId, classroomId, subjectId);
        List<ImportRowData> importedRows = readImportRows(file, context.weekConfigs(), schoolYearId, unitId,
                classroomId, subjectId);
        replaceProgramDistributions(context, importedRows);

        return ProgramDistributionImportResultDto.builder()
                .successCount(importedRows.size())
                .failedCount(0)
                .build();
    }

    private ExportContext buildContext(Long schoolYearId, Long unitId, Long classroomId, Long subjectId) {
        SchoolYear schoolYear = schoolYearRepository.findById(schoolYearId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
        Unit unit = unitRepository.findById(unitId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        Classroom classroom = classroomRepository.findById(classroomId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
        Subject subject = subjectRepository.findById(subjectId)
                .filter(item -> item.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));

        if (!classroom.getSchoolYear().getId().equals(schoolYearId)) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_CLASSROOM_SCHOOL_YEAR_MISMATCH);
        }
        if (!classroom.getUnit().getId().equals(unitId)) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_CLASSROOM_UNIT_MISMATCH);
        }
        if (!classroomSubjectRepository.existsByClassroomIdAndSubjectId(classroomId, subjectId)) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_SUBJECT_NOT_ASSIGNED_TO_CLASSROOM);
        }

        List<WeekConfig> weekConfigs = weekConfigRepository
                .search(schoolYearId, null);
        if (weekConfigs.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_TEMPLATE_DATA_NOT_FOUND);
        }

        Map<Integer, WeekConfig> weekConfigByWeekNumber = new HashMap<>();
        for (WeekConfig weekConfig : weekConfigs) {
            weekConfigByWeekNumber.put(weekConfig.getWeekNumber(), weekConfig);
        }

        return new ExportContext(schoolYear, unit, classroom, subject, weekConfigs, weekConfigByWeekNumber);
    }

    private List<TemplateRowData> buildTemplateRows(List<WeekConfig> weekConfigs,
            List<ProgramDistribution> existingItems) {
        if (!existingItems.isEmpty()) {
            return existingItems.stream()
                    .map(item -> new TemplateRowData(item.getOrderNumber(), item.getWeekNumber(), item.getPeriodPpct(),
                            item.getLessonName(), item.getNote()))
                    .toList();
        }

        List<TemplateRowData> rows = new ArrayList<>();
        int order = 1;
        for (WeekConfig weekConfig : weekConfigs) {
            rows.add(new TemplateRowData(order++, weekConfig.getWeekNumber(), "", "", ""));
        }
        return rows;
    }

    private void buildTemplateSheet(Workbook workbook, Sheet sheet, ExportContext context, List<TemplateRowData> rows) {
        CellStyle topStyle = createTopStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle subtitleStyle = createSubtitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook);
        CellStyle guideStyle = createGuideStyle(workbook);

        // Lưu metadata: SchoolYearId, ClassroomId, SubjectId vào các cell ẩn (column G)
        createCell(sheet.createRow(0), 6, "SchoolYearId", null);
        createCell(sheet.getRow(0), 7, context.schoolYear().getId(), bodyStyle);

        createCell(sheet.createRow(1), 6, "UnitId", null);
        createCell(sheet.getRow(1), 7, context.unit().getId(), bodyStyle);

        createCell(sheet.createRow(2), 6, "ClassroomId", null);
        createCell(sheet.getRow(2), 7, context.classroom().getId(), bodyStyle);

        createCell(sheet.createRow(3), 6, "SubjectId", null);
        createCell(sheet.getRow(3), 7, context.subject().getId(), bodyStyle);

        // Ẩn column G và H (metadata)
        sheet.setColumnWidth(6, 0);
        sheet.setColumnWidth(7, 0);

        // Dòng 1: Đơn vị
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        Row unitRow = sheet.getRow(0) != null ? sheet.getRow(0) : sheet.createRow(0);
        createCell(unitRow, 0, context.unit().getName(), topStyle);

        // Dòng 2: Trống
        if (sheet.getRow(1) == null) {
            sheet.createRow(1);
        }

        // Dòng 3: Tiêu đề
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 4));
        Row titleRow = sheet.getRow(2) != null ? sheet.getRow(2) : sheet.createRow(2);
        createCell(titleRow, 0, "BẢNG PHÂN PHỐI CHƯƠNG TRÌNH", titleStyle);

        // Dòng 4: Thông tin chi tiết
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 4));
        Row subtitleRow = sheet.getRow(3) != null ? sheet.getRow(3) : sheet.createRow(3);
        String subtitle = context.subject().getName() + " • " + context.classroom().getName() + " • "
                + context.schoolYear().getName();
        createCell(subtitleRow, 0, subtitle, subtitleStyle);

        // Dòng 5: Hướng dẫn sử dụng
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 4));
        Row guideRow = sheet.createRow(4);
        createCell(guideRow, 0, "Hướng dẫn: Không sửa tiêu đề của mẫu", guideStyle);

        // Header row
        Row headerRow = sheet.createRow(HEADER_ROW_INDEX);
        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Tuần", headerStyle);
        createCell(headerRow, 2, "Tiết PPCT", headerStyle);
        createCell(headerRow, 3, "Tên bài học", headerStyle);
        createCell(headerRow, 4, "Ghi chú", headerStyle);

        // Add detailed comments for each header
        addComment(sheet, headerRow.getCell(0),
                "⚠️ Thứ tự bài học\nKhông được sửa");
        addComment(sheet, headerRow.getCell(1),
                "📋 Cấu hình tuần\nCó thể tái sử dụng cho cả 2 học kì\n1 tuần có thể nhiều dòng");
        addComment(sheet, headerRow.getCell(2),
                "✏️ Tiết PPCT\nGhi 1, 2, 3, 4... từng số");
        addComment(sheet, headerRow.getCell(3),
                "✏️ Tên bài học (BẮT BUỘC)\n• Tối đa 250 ký tự\n• Không được bỏ trống\n• Ví dụ: Bài 1: Giới thiệu");
        addComment(sheet, headerRow.getCell(4),
                "✏️ Ghi chú (Tùy chọn)\n• Các lưu ý bổ sung");

        // Data rows
        int rowIndex = DATA_START_ROW_INDEX;
        for (TemplateRowData item : rows) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, item.orderNumber(), bodyStyle);
            createCell(row, 1, item.weekNumber(), bodyStyle);
            createCell(row, 2, item.periodPpct(), bodyStyle);
            createCell(row, 3, item.lessonName(), bodyStyle);
            createCell(row, 4, item.note(), bodyStyle);
        }

        // Set column widths
        sheet.setColumnWidth(0, 8 * 256); // STT
        sheet.setColumnWidth(1, 10 * 256); // Tuần
        sheet.setColumnWidth(2, 15 * 256); // Tiết PPCT
        sheet.setColumnWidth(3, 50 * 256); // Tên bài học
        sheet.setColumnWidth(4, 35 * 256); // Ghi chú

        // Freeze panes
        sheet.createFreezePane(0, HEADER_ROW_INDEX + 1);
    }

    private CellStyle createTopStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        font.setFontName("Times New Roman");

        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        font.setFontName("Times New Roman");

        style.setFont(font);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        font.setFontName("Times New Roman");

        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontName("Times New Roman");
        style.setFont(font);

        return style;
    }

    private CellStyle createGuideStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.ORANGE.getIndex());
        font.setFontName("Times New Roman");
        style.setFont(font);
        style.setWrapText(true);

        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Times New Roman");
        style.setFont(font);

        return style;
    }

    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }
    }

    private List<ImportRowData> readImportRows(MultipartFile file, List<WeekConfig> weekConfigs,
            Long schoolYearId, Long unitId, Long classroomId, Long subjectId) {
        Set<Integer> validWeekNumbers = weekConfigs.stream().map(WeekConfig::getWeekNumber).collect(HashSet::new,
                Set::add, Set::addAll);
        List<ImportRowData> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        Integer autoOrderNumber = 1;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Validate metadata from file
            validateFileMetadata(sheet, formatter, schoolYearId, unitId, classroomId, subjectId);

            for (int i = DATA_START_ROW_INDEX; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                Integer orderNumber = readIntegerCell(row.getCell(0), formatter);
                Integer weekNumber = readIntegerCell(row.getCell(1), formatter);
                String periodPpct = readStringCell(row.getCell(2), formatter);
                String lessonName = readStringCell(row.getCell(3), formatter);
                String note = readStringCell(row.getCell(4), formatter);

                // Auto-generate orderNumber if not provided
                if (orderNumber == null) {
                    orderNumber = autoOrderNumber;
                }
                autoOrderNumber++;

                if (weekNumber == null) {
                    throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
                }
                if (!validWeekNumbers.contains(weekNumber)) {
                    throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_WEEK_NOT_FOUND);
                }
                if (lessonName == null || lessonName.isBlank()) {
                    throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_LESSON_NAME_REQUIRED);
                }

                rows.add(new ImportRowData(orderNumber, weekNumber, trimToNull(periodPpct), lessonName.trim(),
                        trimToNull(note)));
            }
        } catch (IOException ex) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }

        if (rows.isEmpty()) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }

        return rows;
    }

    private void validateFileMetadata(Sheet sheet, DataFormatter formatter, Long schoolYearId, Long unitId,
            Long classroomId, Long subjectId) {
        // Đọc metadata từ các cell ẩn (column G, H)
        Row row0 = sheet.getRow(0);
        Row row1 = sheet.getRow(1);
        Row row2 = sheet.getRow(2);
        Row row3 = sheet.getRow(3);

        if (row0 == null || row1 == null || row2 == null || row3 == null) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }

        try {
            Long fileSchoolYearId = readLongCell(row0.getCell(7), formatter);
            Long fileUnitId = readLongCell(row1.getCell(7), formatter);
            Long fileClassroomId = readLongCell(row2.getCell(7), formatter);
            Long fileSubjectId = readLongCell(row3.getCell(7), formatter);

            if (fileSchoolYearId == null || fileUnitId == null || fileClassroomId == null || fileSubjectId == null) {
                throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
            }

            // So sánh với parameters
            if (!fileSchoolYearId.equals(schoolYearId)
                    || !fileUnitId.equals(unitId)
                    || !fileClassroomId.equals(classroomId)
                    || !fileSubjectId.equals(subjectId)) {
                throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
            }
        } catch (UserMessageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }
    }

    private void addComment(Sheet sheet, Cell cell, String commentText) {
        CreationHelper factory = sheet.getWorkbook().getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();

        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex() + 1);
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 4);

        Comment comment = drawing.createCellComment(anchor);
        RichTextString str = factory.createRichTextString(commentText);
        comment.setString(str);
        comment.setAuthor("System");

        cell.setCellComment(comment);
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int i = 0; i <= 4; i++) {
            if (!readStringCell(row.getCell(i), formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Integer readIntegerCell(Cell cell, DataFormatter formatter) {
        String value = readStringCell(cell, formatter);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }
    }

    private Long readLongCell(Cell cell, DataFormatter formatter) {
        String value = readStringCell(cell, formatter);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_INVALID_FILE);
        }
    }

    private String readStringCell(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private void replaceProgramDistributions(ExportContext context, List<ImportRowData> importedRows) {
        List<ProgramDistribution> existingItems = programDistributionRepository
                .findBySchoolYearIdAndUnitIdAndClassroomIdAndSubjectIdAndDeletedFlagOrderByOrderNumberAscIdAsc(
                        context.schoolYear().getId(), context.unit().getId(), context.classroom().getId(),
                        context.subject().getId(), 0);

        // Map existing items by key (weekNumber:orderNumber) to try to update in-place
        Map<String, ProgramDistribution> existingMap = new HashMap<>();
        for (ProgramDistribution e : existingItems) {
            String key = (e.getWeekNumber() == null ? "" : e.getWeekNumber().toString()) + ":"
                    + (e.getOrderNumber() == null ? "" : e.getOrderNumber().toString());
            existingMap.put(key, e);
        }

        List<ProgramDistribution> toSave = new ArrayList<>();
        Set<Long> matchedIds = new HashSet<>();

        for (ImportRowData importedRow : importedRows) {
            if (context.weekConfigByWeekNumber().get(importedRow.weekNumber()) == null) {
                throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_WEEK_NOT_FOUND);
            }

            String key = (importedRow.weekNumber() == null ? "" : importedRow.weekNumber().toString()) + ":"
                    + (importedRow.orderNumber() == null ? "" : importedRow.orderNumber().toString());

            ProgramDistribution existing = existingMap.get(key);
            if (existing != null) {
                // update existing record in-place to preserve its id
                existing.setWeekNumber(importedRow.weekNumber());
                existing.setOrderNumber(importedRow.orderNumber());
                existing.setPeriodPpct(importedRow.periodPpct());
                existing.setLessonName(importedRow.lessonName());
                existing.setNote(importedRow.note());
                existing.setDeletedFlag(0);
                existing.setUpdatedBy(getCurrentUsername());
                existing.setUpdatedAt(LocalDateTime.now());
                toSave.add(existing);
                matchedIds.add(existing.getId());
            } else {
                // create new
                ProgramDistribution item = new ProgramDistribution();
                item.setSchoolYear(context.schoolYear());
                item.setUnit(context.unit());
                item.setClassroom(context.classroom());
                item.setSubject(context.subject());
                item.setOrderNumber(importedRow.orderNumber());
                item.setWeekNumber(importedRow.weekNumber());
                item.setPeriodPpct(importedRow.periodPpct());
                item.setLessonName(importedRow.lessonName());
                item.setNote(importedRow.note());
                item.setCreatedBy(getCurrentUsername());
                toSave.add(item);
            }
        }

        // Soft-delete existing items that were not matched
        for (ProgramDistribution existingItem : existingItems) {
            if (existingItem.getId() != null && !matchedIds.contains(existingItem.getId())) {
                existingItem.setDeletedFlag(1);
                existingItem.setDeletedAt(LocalDateTime.now());
                existingItem.setDeletedBy(getCurrentUsername());
                existingItem.setUpdatedBy(getCurrentUsername());
                toSave.add(existingItem);
            }
        }

        if (!toSave.isEmpty()) {
            programDistributionRepository.saveAll(toSave);
        }
    }

    private ProgramDistributionItemDto toDto(ProgramDistribution item) {
        return ProgramDistributionItemDto.builder()
                .id(item.getId())
                .schoolYearName(item.getSchoolYear().getName())
                .unitName(item.getUnit().getName())
                .classroomName(item.getClassroom().getName())
                .subjectName(item.getSubject().getName())
                .orderNumber(item.getOrderNumber())
                .weekNumber(item.getWeekNumber())
                .weekName("Tuần " + item.getWeekNumber())
                .periodPpct(item.getPeriodPpct())
                .lessonName(item.getLessonName())
                .note(item.getNote())
                .build();
    }

    private ProgramDistributionDetailDto toDetailDto(ProgramDistribution item) {
        return ProgramDistributionDetailDto.builder()
                .id(item.getId())
                .schoolYearId(item.getSchoolYear().getId())
                .schoolYearName(item.getSchoolYear().getName())
                .unitId(item.getUnit().getId())
                .unitName(item.getUnit().getName())
                .classroomId(item.getClassroom().getId())
                .classroomName(item.getClassroom().getName())
                .subjectId(item.getSubject().getId())
                .subjectName(item.getSubject().getName())
                .orderNumber(item.getOrderNumber())
                .weekNumber(item.getWeekNumber())
                .weekName("Tuần " + item.getWeekNumber())
                .periodPpct(item.getPeriodPpct())
                .lessonName(item.getLessonName())
                .note(item.getNote())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ProgramDistributionItemDto, ProgramDistributionFilterDto> search(
            PageRequestDto<ProgramDistributionFilterDto> request) {
        ProgramDistributionFilterDto filter = request.getFilter() != null ? request.getFilter()
                : new ProgramDistributionFilterDto();
        Integer pageNow = request.getPageNow() != null && request.getPageNow() > 0 ? request.getPageNow() : 1;
        Integer pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 10;

        Page<ProgramDistribution> page = programDistributionRepository.findAll(
                programDistributionSpecification.buildSpecification(
                        filter.getSchoolYearId(),
                        filter.getUnitId(),
                        filter.getClassroomId(),
                        filter.getSubjectId()),
                PageRequest.of(
                        pageNow - 1,
                        pageSize,
                        Sort.by(
                                Sort.Order.asc("orderNumber"),
                                Sort.Order.asc("id"))));

        return PageResponseDto.<ProgramDistributionItemDto, ProgramDistributionFilterDto>builder()
                .pageNow(pageNow)
                .pageSize(pageSize)
                .pageTotal((int) Math.ceil((double) page.getTotalElements() / pageSize))
                .recordTotal(page.getTotalElements())
                .filter(filter)
                .items(page.getContent().stream().map(this::toDto).toList())
                .build();
    }

    @Override
    @Transactional
    public ProgramDistributionDetailDto create(ProgramDistributionCreateRequest request) {
        // Validate and fetch related entities
        SchoolYear schoolYear = schoolYearRepository.findById(request.getSchoolYearId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));

        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));

        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));

        // Validate classroom belongs to unit and school year
        if (!classroom.getUnit().getId().equals(unit.getId())) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_CLASSROOM_UNIT_MISMATCH);
        }
        if (!classroom.getSchoolYear().getId().equals(schoolYear.getId())) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_CLASSROOM_SCHOOL_YEAR_MISMATCH);
        }

        // Validate subject is assigned to classroom
        boolean isSubjectAssigned = classroomSubjectRepository.existsByClassroomIdAndSubjectId(
                classroom.getId(), subject.getId());
        if (!isSubjectAssigned) {
            throw new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_SUBJECT_NOT_ASSIGNED_TO_CLASSROOM);
        }

        // Validate week exists
        weekConfigRepository.findBySchoolYearIdAndWeekNumber(schoolYear.getId(), request.getWeekNumber())
            .orElseThrow(() -> new UserMessageException(CommonErrorCode.PROGRAM_DISTRIBUTION_WEEK_NOT_FOUND));

        ProgramDistribution entity = new ProgramDistribution();
        entity.setSchoolYear(schoolYear);
        entity.setUnit(unit);
        entity.setClassroom(classroom);
        entity.setSubject(subject);
        entity.setOrderNumber(request.getOrderNumber());
        entity.setWeekNumber(request.getWeekNumber());
        entity.setPeriodPpct(request.getPeriodPpct());
        entity.setLessonName(request.getLessonName());
        entity.setNote(request.getNote());
        entity.setCreatedBy(SecurityUtils.getCurrentUsername());

        ProgramDistribution saved = programDistributionRepository.save(entity);
        return toDetailDto(saved);
    }

    @Override
    public ProgramDistributionDetailDto getById(Long id) {
        ProgramDistribution entity = programDistributionRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.RECORD_NOT_FOUND));
        return toDetailDto(entity);
    }

    @Override
    @Transactional
    public ProgramDistributionDetailDto update(Long id, ProgramDistributionUpdateRequest request) {
        ProgramDistribution entity = programDistributionRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.RECORD_NOT_FOUND));

        // Validate weekNumber exists
        weekConfigRepository.findBySchoolYearIdAndWeekNumber(entity.getSchoolYear().getId(), request.getWeekNumber())
            .orElseThrow(() -> new UserMessageException(CommonErrorCode.INVALID_REQUEST));

        entity.setWeekNumber(request.getWeekNumber());
        if (request.getOrderNumber() != null) {
            entity.setOrderNumber(request.getOrderNumber());
        }
        entity.setPeriodPpct(request.getPeriodPpct());
        entity.setLessonName(request.getLessonName());
        entity.setNote(request.getNote());
        entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
        entity.setUpdatedAt(LocalDateTime.now());

        ProgramDistribution saved = programDistributionRepository.save(entity);
        return toDetailDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProgramDistribution entity = programDistributionRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.RECORD_NOT_FOUND));
        entity.setDeletedFlag(1);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedBy(SecurityUtils.getCurrentUsername());

        programDistributionRepository.save(entity);
    }

    private record ExportContext(SchoolYear schoolYear, Unit unit, Classroom classroom, Subject subject,
            List<WeekConfig> weekConfigs, Map<Integer, WeekConfig> weekConfigByWeekNumber) {
    }

    private record TemplateRowData(Integer orderNumber, Integer weekNumber, String periodPpct, String lessonName,
            String note) {
    }

    private record ImportRowData(Integer orderNumber, Integer weekNumber, String periodPpct, String lessonName,
            String note) {
    }
}
