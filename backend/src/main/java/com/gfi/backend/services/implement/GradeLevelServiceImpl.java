package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelCreateRequest;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelDetailDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelFilterDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelListItemDto;
import com.gfi.backend.models.dtos.gradelevel.GradeLevelUpdateRequest;
import com.gfi.backend.models.entities.GradeLevel;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.GradeLevelRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.specifications.GradeLevelSpecification;
import com.gfi.backend.services.interfaces.GradeLevelService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic quản lý khối lớp.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: GradeLevelSpecification
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class GradeLevelServiceImpl implements GradeLevelService {

    private final GradeLevelRepository gradeLevelRepository;
    private final ClassroomRepository classroomRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;
    private final GradeLevelSpecification gradeLevelSpecification;

    // Tìm kiếm và phân trang khối lớp với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<GradeLevelListItemDto, GradeLevelFilterDto> search(
            PageRequestDto<GradeLevelFilterDto> request) {
        GradeLevelFilterDto filter = request.getFilter() == null ? new GradeLevelFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<GradeLevel> page = gradeLevelRepository.findAll(gradeLevelSpecification.buildSpecification(filter),
                pageable);
        List<GradeLevelListItemDto> items = page.getContent().stream().map(this::toListItemDto).toList();

        return PageResponseDto.<GradeLevelListItemDto, GradeLevelFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    // Danh sách khối lớp cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return gradeLevelRepository
                .findAll(Sort.by(Sort.Direction.ASC, "gradeNumber").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    // Chi tiết khối lớp theo ID
    @Override
    @Transactional(readOnly = true)
    public GradeLevelDetailDto getById(Long id) {
        return toDetailDto(findGradeLevel(id));
    }

    // Thêm mới khối lớp
    @Override
    @Transactional
    public GradeLevelDetailDto create(GradeLevelCreateRequest request) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        Integer gradeNumber = request.getGradeNumber();

        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);
        ensureGradeNumberUnique(gradeNumber, null);

        GradeLevel gradeLevel = new GradeLevel();
        gradeLevel.setCode(code);
        gradeLevel.setName(name);
        gradeLevel.setGradeNumber(gradeNumber);
        gradeLevel.setStatus(request.getStatus());
        gradeLevel.setDescription(normalizeNullable(request.getDescription()));
        gradeLevel.setCreatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(gradeLevelRepository.save(gradeLevel));
    }

    // Cập nhật khối lớp
    @Override
    @Transactional
    public GradeLevelDetailDto update(Long id, GradeLevelUpdateRequest request) {
        GradeLevel gradeLevel = findGradeLevel(id);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        Integer gradeNumber = request.getGradeNumber();

        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);
        ensureGradeNumberUnique(gradeNumber, id);

        gradeLevel.setCode(code);
        gradeLevel.setName(name);
        gradeLevel.setGradeNumber(gradeNumber);
        gradeLevel.setStatus(request.getStatus());
        gradeLevel.setDescription(normalizeNullable(request.getDescription()));
        gradeLevel.setUpdatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(gradeLevelRepository.save(gradeLevel));
    }

    // Xóa khối lớp (soft delete). Kiểm tra không được xóa nếu còn lớp học hoặc cấu
    // hình môn học.
    @Override
    @Transactional
    public void delete(Long id) {
        GradeLevel gradeLevel = findGradeLevel(id);
        if (classroomRepository.countByGradeLevelId(id) > 0
                || gradeLevelSubjectRepository.countByGradeLevelId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_IN_USE);
        }

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        gradeLevel.setDeletedFlag(1);
        gradeLevel.setDeletedAt(LocalDateTime.now());
        gradeLevel.setDeletedBy(SecurityUtils.getCurrentUsername());
        gradeLevelRepository.save(gradeLevel);
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.GRADE_LEVEL_NOT_FOUND));
    }

    // Kiểm tra mã khối lớp phải duy nhất
    private void ensureCodeUnique(String code, Long id) {
        gradeLevelRepository.findByCode(code)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_CODE_ALREADY_EXISTS);
                });
    }

    // Kiểm tra tên khối lớp phải duy nhất
    private void ensureNameUnique(String name, Long id) {
        gradeLevelRepository.findByName(name)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_NAME_ALREADY_EXISTS);
                });
    }

    // Kiểm tra thứ tự khối lớp phải duy nhất
    private void ensureGradeNumberUnique(Integer gradeNumber, Long id) {
        gradeLevelRepository.findByGradeNumber(gradeNumber)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.GRADE_LEVEL_NUMBER_ALREADY_EXISTS);
                });
    }

    private GradeLevelDetailDto toDetailDto(GradeLevel gradeLevel) {
        return GradeLevelDetailDto.builder()
                .id(gradeLevel.getId())
                .code(gradeLevel.getCode())
                .name(gradeLevel.getName())
                .gradeNumber(gradeLevel.getGradeNumber())
                .status(gradeLevel.getStatus())
                .description(gradeLevel.getDescription())
                .build();
    }

    private GradeLevelListItemDto toListItemDto(GradeLevel gradeLevel) {
        return GradeLevelListItemDto.builder()
                .id(gradeLevel.getId())
                .code(gradeLevel.getCode())
                .name(gradeLevel.getName())
                .gradeNumber(gradeLevel.getGradeNumber())
                .status(gradeLevel.getStatus())
                .build();
    }

    /**
     * Kiểm tra string có nội dung hay không.
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Chuẩn hóa string: trim.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Chuẩn hóa string nullable: return null nếu rỗng hoặc whitespace.
     */
    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20; // Default page size
        }
        return Math.min(pageSize, 100); // Max 100 items per page
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        if (pageNow == null || pageNow < 0) {
            return 0; // Default first page
        }
        return pageNow;
    }

    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final java.time.format.DateTimeFormatter EXPORT_TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional(readOnly = true)
    public byte[] export(PageRequestDto<GradeLevelFilterDto> request, com.gfi.backend.models.enums.ExportType exportType) {
        GradeLevelFilterDto filter = request.getFilter() == null ? new GradeLevelFilterDto() : request.getFilter();
        List<GradeLevel> items = gradeLevelRepository.findAll(
                gradeLevelSpecification.buildSpecification(filter),
                Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")));

        if (exportType == com.gfi.backend.models.enums.ExportType.PDF) {
            return exportGradeLevelsPdf(items);
        }
        return exportGradeLevelsExcel(items);
    }

    private byte[] exportGradeLevelsExcel(java.util.List<GradeLevel> items) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("KhoiLop");
            
            org.apache.poi.ss.usermodel.Row infoRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("Thời gian tải: " + java.time.LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + " | Người tải: " + com.gfi.backend.utils.SecurityUtils.getCurrentUsername());
            org.apache.poi.ss.usermodel.CellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
            infoStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font infoFont = workbook.createFont();
            infoFont.setFontName(EXPORT_FONT_NAME);
            infoStyle.setFont(infoFont);
            infoCell.setCellStyle(infoStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));
            
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH KHỐI LỚP");
            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setFontName(EXPORT_FONT_NAME);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(3);
            String[] headers = { "STT", "Mã khối", "Tên khối", "Mô tả", "Trạng thái" };
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor((short) 41);
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName(EXPORT_FONT_NAME);
            headerStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            org.apache.poi.ss.usermodel.CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
            bodyStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            bodyStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            bodyStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            bodyStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            bodyStyle.setWrapText(true);
            org.apache.poi.ss.usermodel.Font bodyFont = workbook.createFont();
            bodyFont.setFontName(EXPORT_FONT_NAME);
            bodyStyle.setFont(bodyFont);

            int rowIndex = 4;
            int stt = 1;
            for (GradeLevel item : items) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex++);
                int col = 0;
                org.apache.poi.ss.usermodel.Cell c = row.createCell(col++); c.setCellValue(stt++); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getCode()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getName()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getDescription()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(Integer.valueOf(1).equals(item.getStatus()) ? "Hoạt động" : "Không hoạt động"); c.setCellStyle(bodyStyle);
            }
            
            for(int i=0; i<5; i++) sheet.autoSizeColumn(i);
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (java.io.IOException ex) {
            throw new com.gfi.backend.controllers.exceptions.UserMessageException("Không thể tạo file Excel");
        }
    }

    private byte[] exportGradeLevelsPdf(java.util.List<GradeLevel> items) {
        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 24, 24, 20, 20);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font infoFont = createPdfFont(10, com.lowagie.text.Font.ITALIC);

            com.lowagie.text.Paragraph exportInfo = new com.lowagie.text.Paragraph("Thời gian tải: " + java.time.LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + " | Người tải: " + com.gfi.backend.utils.SecurityUtils.getCurrentUsername(), infoFont);
            exportInfo.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            exportInfo.setSpacingAfter(6f);
            document.add(exportInfo);

            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("DANH SÁCH KHỐI LỚP", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(new float[] { 1.0f, 2.5f, 3.5f, 3.5f, 2.0f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Mã khối", headerFont);
            addPdfHeaderCell(table, "Tên khối", headerFont);
            addPdfHeaderCell(table, "Mô tả", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);

            int stt = 1;
            for (GradeLevel item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, com.lowagie.text.Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, com.lowagie.text.Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, com.lowagie.text.Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getDescription(), bodyFont, com.lowagie.text.Element.ALIGN_LEFT);
                addPdfBodyCell(table, Integer.valueOf(1).equals(item.getStatus()) ? "Hoạt động" : "Không hoạt động", bodyFont, com.lowagie.text.Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (com.lowagie.text.DocumentException | java.io.IOException ex) {
            throw new com.gfi.backend.controllers.exceptions.UserMessageException("Không thể tạo file PDF");
        }
    }

    private void addPdfHeaderCell(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new java.awt.Color(224, 242, 241));
        table.addCell(cell);
    }

    private void addPdfBodyCell(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
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
            if (java.nio.file.Files.exists(java.nio.file.Path.of(fontPath))) {
                com.lowagie.text.pdf.BaseFont baseFont = com.lowagie.text.pdf.BaseFont.createFont(fontPath, com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
                return new com.lowagie.text.Font(baseFont, size, com.lowagie.text.Font.NORMAL);
            }
        } catch (Exception ignored) {}
        return com.lowagie.text.FontFactory.getFont(EXPORT_FONT_NAME, com.lowagie.text.pdf.BaseFont.IDENTITY_H, true, size, style);
    }
}
