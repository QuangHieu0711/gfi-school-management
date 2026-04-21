package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.common.PageRequestDto;
import com.gfi.backend.models.dtos.common.PageResponseDto;
import com.gfi.backend.models.dtos.unit.UnitCreateRequest;
import com.gfi.backend.models.dtos.unit.UnitDetailDto;
import com.gfi.backend.models.dtos.unit.UnitFilterDto;
import com.gfi.backend.models.dtos.unit.UnitImportResultDto;
import com.gfi.backend.models.dtos.unit.UnitListItemDto;
import com.gfi.backend.models.dtos.unit.UnitUpdateRequest;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.models.enums.ActionType;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.enums.ScopeType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.models.mappers.UnitMapper;
import com.gfi.backend.models.security.ResolvedScope;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.repositories.specifications.UnitSpecification;
import com.gfi.backend.services.interfaces.DataScopeFilterService;
import com.gfi.backend.services.interfaces.UnitService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;
import com.gfi.backend.models.security.FeatureKey;

import lombok.RequiredArgsConstructor;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Service xử lý logic quản lý đơn vị.
 * 
 * Trách nhiệm tách biệt:
 * - Logic query: UnitSpecification
 * - Logic mapping: UnitMapper
 * - Validate & load relations: private helpers
 * - Security: SecurityUtils
 */
@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {
    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final UnitSpecification unitSpecification;
    private final UnitMapper unitMapper;
    private final DataScopeFilterService dataScopeFilterService;
    
    // Feature key cho phân quyền
    private static final String FEATURE = FeatureKey.UNIT_MANAGEMENT.getCode();

    // Tìm kiếm và phân trang units với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UnitListItemDto, UnitFilterDto> search(PageRequestDto<UnitFilterDto> request) {
        UnitFilterDto filter = request.getFilter() == null ? new UnitFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);

        Page<Unit> page = unitRepository.findAll(unitSpecification.buildSpecification(filter, resolvedScopes), pageable);
        List<UnitListItemDto> items = page.getContent().stream()
                .map(unitMapper::toListItemDto)
                .toList();

        return PageResponseDto.<UnitListItemDto, UnitFilterDto>builder()
                .pageSize(pageSize)
                .pageNow(pageNow)
                .filter(filter)
                .pageTotal(page.getTotalPages())
                .recordTotal(page.getTotalElements())
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] export(PageRequestDto<UnitFilterDto> request, ExportType exportType) {
        UnitFilterDto filter = request == null || request.getFilter() == null ? new UnitFilterDto() : request.getFilter();
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        List<Unit> items = unitRepository
                .findAll(unitSpecification.buildSpecification(filter, resolvedScopes), Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")))
                .toList();

        return switch (exportType) {
            case EXCEL -> exportUnitsExcel(items);
            case PDF -> exportUnitsPdf(items);
        };
    }

    @Override
    @Transactional
    public UnitImportResultDto importExcel(MultipartFile file) {
        validateExcelFile(file);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("DonVi");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new UserMessageException("File Excel không có sheet DonVi");
            }

            DataFormatter formatter = new DataFormatter();
            int successCount = 0;

            for (int rowIndex = 6; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String code = readCellText(row.getCell(1), formatter);
                String name = readCellText(row.getCell(2), formatter);
                String address = readCellText(row.getCell(3), formatter);
                String phone = readCellText(row.getCell(4), formatter);
                String email = readCellText(row.getCell(5), formatter);
                String statusText = readCellText(row.getCell(6), formatter);

                if (!StringUtils.hasText(code) && !StringUtils.hasText(name)) {
                    continue;
                }
                if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
                    throw new UserMessageException("Mã đơn vị và tên đơn vị là bắt buộc tại dòng " + (rowIndex + 1));
                }

                Unit unit = unitRepository.findByCode(code.trim())
                        .orElseGet(Unit::new);
                unit.setCode(code.trim());
                unit.setName(name.trim());
                unit.setAddress(normalizeNullable(address));
                unit.setPhone(normalizeNullable(phone));
                unit.setEmail(normalizeNullable(email));
                unit.setStatus(parseStatus(statusText));
                unit.setDeletedFlag(0);
                if (unit.getId() == null) {
                    unit.setCreatedBy(SecurityUtils.getCurrentUsername());
                } else {
                    unit.setUpdatedBy(SecurityUtils.getCurrentUsername());
                }
                unitRepository.save(unit);
                successCount++;
            }

            return UnitImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(0)
                    .build();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file Excel đơn vị");
        }
    }

    // Lấy danh sách đơn vị cho dropdown/combobox
    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        UnitFilterDto filter = new UnitFilterDto();
        filter.setStatus(1);
        List<Unit> units = unitRepository.findAll(unitSpecification.buildSpecification(filter, resolvedScopes),
                Sort.by(Sort.Direction.ASC, "name"));

        return units.stream()
                .map(unit -> LookupItemDto.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .build())
                .toList();
    }

    // Chi tiết đơn vị theo ID
    @Override
    @Transactional(readOnly = true)
    public UnitDetailDto getById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        validateUnitScope(ActionType.VIEW, unit.getId());
        return unitMapper.toDetailDto(unit);
    }

    // Thêm mới đơn vị
    @Override
    @Transactional
    public UnitDetailDto create(UnitCreateRequest request) {
        String code = normalize(request.getCode());
        validateCodeDuplicate(code, null);

        Unit unit = new Unit();
        unit.setCode(code);
        unit.setName(normalize(request.getName()));
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setStatus(request.getStatus());
        unit.setCreatedBy(SecurityUtils.getCurrentUsername());

        return unitMapper.toDetailDto(unitRepository.save(unit));
    }

    // Cập nhật đơn vị
    @Override
    @Transactional
    public UnitDetailDto update(Long id, UnitUpdateRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        validateUnitScope(ActionType.EDIT, unit.getId());

        String code = normalize(request.getCode());
        validateCodeDuplicate(code, id);

        unit.setCode(code);
        unit.setName(normalize(request.getName()));
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setStatus(request.getStatus());
        unit.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return unitMapper.toDetailDto(unitRepository.save(unit));
    }

    // Xóa đơn vị (xóa mềm)
    @Override
    @Transactional
    public void delete(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.UNIT_NOT_FOUND));
        validateUnitScope(ActionType.DELETE, unit.getId());

        // Kiểm tra unit có được sử dụng không
        if (userRepository.findByUnitIdIn(List.of(id)).size() > 0) {
            throw new UserMessageException(CommonErrorCode.UNIT_IN_USE);
        }
        if (classroomRepository.countByUnitId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.UNIT_IN_USE);
        }

        // Xóa mềm: đánh dấu xóa
        unit.setDeletedFlag(1);
        unit.setDeletedAt(LocalDateTime.now());
        unit.setDeletedBy(SecurityUtils.getCurrentUsername());
        unitRepository.save(unit);
    }

    /**
     * Validate code không trùng.
     * Khi update: excludeId cho phép unit giữ nguyên code của chính nó.
     * 
     * @param code mã unit cần check
     * @param excludeId ID unit loại trừ (null khi create)
     */
    private void validateCodeDuplicate(String code, Long excludeId) {
        boolean isDuplicate = excludeId == null
                ? unitRepository.existsByCode(code)
                : unitRepository.existsByCodeAndIdNot(code, excludeId);
        
        if (isDuplicate) {
            throw new UserMessageException(CommonErrorCode.UNIT_CODE_ALREADY_EXISTS);
        }
    }

    private void validateUnitScope(ActionType action, Long unitId) {
        dataScopeFilterService.checkDataScopeAccess(FEATURE, action, ScopeType.UNIT, unitId);
    }

    private byte[] exportUnitsExcel(List<Unit> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DonVi");
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle infoStyle = createExportInfoStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);
            CellStyle guideStyle = createGuideStyle(workbook);

            Row infoRow = sheet.createRow(0);
            createCell(infoRow, 0, buildExportInfoLine(), infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row titleRow = sheet.createRow(1);
            createCell(titleRow, 0, "DANH SÁCH ĐƠN VỊ", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            Row guide1 = sheet.createRow(2);
            createCell(guide1, 0, "Hướng dẫn: Có thể sửa trực tiếp file này rồi import lại bằng API import-excel.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 6));

            Row guide2 = sheet.createRow(3);
            createCell(guide2, 0, "Cột bắt buộc: Mã đơn vị, Tên đơn vị. Trạng thái nhận: 1/0 hoặc Hoạt động/Không hoạt động.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 6));

            Row guide3 = sheet.createRow(4);
            createCell(guide3, 0, "Import sẽ cập nhật theo mã đơn vị nếu đã tồn tại, chưa có sẽ tạo mới.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 6));

            Row headerRow = sheet.createRow(5);
            String[] headers = { "STT", "Mã đơn vị", "Tên đơn vị", "Địa chỉ", "Điện thoại", "Email", "Trạng thái" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 6;
            int stt = 1;
            for (Unit item : items) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, stt++, bodyStyle);
                createCell(row, 1, item.getCode(), bodyStyle);
                createCell(row, 2, item.getName(), bodyStyle);
                createCell(row, 3, item.getAddress(), bodyStyle);
                createCell(row, 4, item.getPhone(), bodyStyle);
                createCell(row, 5, item.getEmail(), bodyStyle);
                createCell(row, 6, statusLabel(item.getStatus()), bodyStyle);
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 34 * 256);
            sheet.setColumnWidth(3, 38 * 256);
            sheet.setColumnWidth(4, 20 * 256);
            sheet.setColumnWidth(5, 28 * 256);
            sheet.setColumnWidth(6, 18 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel danh sách đơn vị");
        }
    }

    private byte[] exportUnitsPdf(List<Unit> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 20, 20);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font infoFont = createPdfFont(10, com.lowagie.text.Font.ITALIC);

            Paragraph exportInfo = new Paragraph(buildExportInfoLine(), infoFont);
            exportInfo.setAlignment(Element.ALIGN_RIGHT);
            exportInfo.setSpacingAfter(6f);
            document.add(exportInfo);

            Paragraph title = new Paragraph("DANH SÁCH ĐƠN VỊ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.8f, 1.8f, 2.6f, 3.0f, 1.8f, 2.4f, 1.4f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Mã đơn vị", headerFont);
            addPdfHeaderCell(table, "Tên đơn vị", headerFont);
            addPdfHeaderCell(table, "Địa chỉ", headerFont);
            addPdfHeaderCell(table, "Điện thoại", headerFont);
            addPdfHeaderCell(table, "Email", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);

            int stt = 1;
            for (Unit item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getAddress(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getPhone(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getEmail(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, statusLabel(item.getStatus()), bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF danh sách đơn vị");
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
        } catch (Exception ignored) {
        }

        return com.lowagie.text.FontFactory.getFont(EXPORT_FONT_NAME, BaseFont.IDENTITY_H, true, size, style);
    }

    private CellStyle createExportHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor((short) 41);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExportTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExportInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createGuideStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExportBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private Cell createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
        return cell;
    }

    private String readCellText(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new UserMessageException("File import phải là file Excel .xlsx");
        }
    }

    private Integer parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return 1;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "hoạt động", "hoat dong", "active" -> 1;
            case "0", "không hoạt động", "khong hoat dong", "inactive" -> 0;
            default -> throw new UserMessageException("Trạng thái không hợp lệ: " + value);
        };
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "Hoạt động" : "Không hoạt động";
    }

    private String buildExportInfoLine() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
    }

    /**
     * Chuẩn hóa kích thước trang phân trang.
     */
    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    /**
     * Chuẩn hóa số trang hiện tại.
     */
    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
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
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
