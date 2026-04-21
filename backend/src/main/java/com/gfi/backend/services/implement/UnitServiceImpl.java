package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
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
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import com.gfi.backend.services.interfaces.UnitService;
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
 * Service xá»­ lÃ½ logic quáº£n lÃ½ Ä‘Æ¡n vá»‹.
 * 
 * TrÃ¡ch nhiá»‡m tÃ¡ch biá»‡t:
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
    private final ImportErrorFileStorageService importErrorFileStorageService;
    
    // Feature key cho phân quyền
    private static final String FEATURE = FeatureKey.UNIT_MANAGEMENT.getCode();

    // Tìm kiếm và phân trang đơn vị với filter
    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UnitListItemDto, UnitFilterDto> search(PageRequestDto<UnitFilterDto> request) {
        UnitFilterDto filter = request.getFilter() == null ? new UnitFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageNow - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
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
        List<Unit> items = unitRepository.findAll(
                unitSpecification.buildSpecification(filter, resolvedScopes),
                Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")));

        return switch (exportType) {
            case EXCEL -> exportUnitsExcel(items);
            case PDF -> exportUnitsPdf(items);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate() {
        return exportUnitsExcelTemplate();
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
            int failedCount = 0;
            int dataStartRowIndex = findUnitImportDataStartRow(sheet, formatter);
            Map<Integer, String> rowErrors = new LinkedHashMap<>();

            for (int rowIndex = dataStartRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
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
                    failedCount++;
                    rowErrors.put(rowIndex, "Mã đơn vị và tên đơn vị là bắt buộc");
                    continue;
                }

                String normalizedCode = code.trim();
                if (unitRepository.findByCode(normalizedCode).isPresent()) {
                    failedCount++;
                    rowErrors.put(rowIndex, "Mã đơn vị đã tồn tại");
                    continue;
                }

                try {
                    Unit unit = new Unit();
                    unit.setCode(normalizedCode);
                    unit.setName(name.trim());
                    unit.setAddress(normalizeNullable(address));
                    unit.setPhone(normalizeNullable(phone));
                    unit.setEmail(normalizeNullable(email));
                    unit.setStatus(parseStatus(statusText));
                    unit.setDeletedFlag(0);
                    unit.setCreatedBy(SecurityUtils.getCurrentUsername());
                    unitRepository.save(unit);
                    successCount++;
                } catch (UserMessageException ex) {
                    failedCount++;
                    rowErrors.put(rowIndex, ex.getMessage());
                }
            }

            String errorFileToken = null;
            String errorFileName = null;
            if (!rowErrors.isEmpty()) {
                byte[] errorFileContent = buildUnitImportErrorFile(workbook, sheet, rowErrors);
                errorFileName = "unit_import_error_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
                errorFileToken = importErrorFileStorageService.store(
                        errorFileName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        errorFileContent);
            }

            return UnitImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .hasErrorFile(!rowErrors.isEmpty())
                    .errorFileName(errorFileName)
                    .errorFileToken(errorFileToken)
                    .build();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file Excel đơn vị");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }

    // Láº¥y danh sÃ¡ch Ä‘Æ¡n vá»‹ cho dropdown/combobox
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

    // Chi tiáº¿t Ä‘Æ¡n vá»‹ theo ID
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

    /*
    * Cập nhật đơn vị
     */
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

    // Xóa mềm
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
     * Validate code khoảng trắng thừa và chuyển thành chữ hoa để chuẩn hóa.
     * Khi update: excludeId cho phép giữ nguyên code hiện tại nếu không thay đổi.
     * 
     * @param code mã đơn vị cần check
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

            Row infoRow = sheet.createRow(0);
            createCell(infoRow, 0, buildExportInfoLine(), infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row titleRow = sheet.createRow(1);
            createCell(titleRow, 0, "DANH SÃCH ÄÆ N Vá»Š", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            Row headerRow = sheet.createRow(3);
            String[] headers = { "STT", "MÃ£ Ä‘Æ¡n vá»‹", "TÃªn Ä‘Æ¡n vá»‹", "Äá»‹a chá»‰", "Äiá»‡n thoáº¡i", "Email", "Tráº¡ng thÃ¡i" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 4;
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
            throw new UserMessageException("KhÃ´ng thá»ƒ táº¡o file Excel danh sÃ¡ch Ä‘Æ¡n vá»‹");
        }
    }

    private byte[] exportUnitsExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DonVi");
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);
            CellStyle guideStyle = createGuideStyle(workbook);

            Row titleRow = sheet.createRow(0);
            createCell(titleRow, 0, "IMPORT ĐƠN VỊ", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row guide1 = sheet.createRow(1);
            createCell(guide1, 0, "Hướng dẫn: Có thể chỉnh sửa trực tiếp file này, sau đó tải file lên để nhập dữ liệu.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            Row guide2 = sheet.createRow(2);
            createCell(guide2, 0, "Các cột bắt buộc: Mã đơn vị, Tên đơn vị. Giá trị hợp lệ của cột Trạng thái: Hoạt động / Không hoạt động.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 6));

            Row guide3 = sheet.createRow(3);
            createCell(guide3, 0, "Mã đơn vị không được trùng với mã đã tồn tại trong hệ thống. Nếu mã đã tồn tại, hệ thống sẽ từ chối cập nhật/tạo mới.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 6));

            Row headerRow = sheet.createRow(4);
            String[] headers = { "STT", "Mã đơn vị", "Tên đơn vị", "Địa chỉ", "Điện thoại", "Email", "Trạng thái" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            Row sampleRow = sheet.createRow(5);
            createCell(sampleRow, 0, 1, bodyStyle);
            createCell(sampleRow, 1, "THVN001", bodyStyle);
            createCell(sampleRow, 2, "Trường Tiểu học Mẫu", bodyStyle);
            createCell(sampleRow, 3, "123 Đường ABC", bodyStyle);
            createCell(sampleRow, 4, "0901000001", bodyStyle);
            createCell(sampleRow, 5, "thvn001@example.com", bodyStyle);
            createCell(sampleRow, 6, "Hoạt động", bodyStyle);

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
            throw new UserMessageException("Không thể tạo file Excel mẫu đơn vị");
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
    private byte[] buildUnitImportErrorFile(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors) {
        CellStyle resultHeaderStyle = createImportResultHeaderStyle(workbook);
        CellStyle errorCellStyle = createImportErrorCellStyle(workbook);
        int resultColumnIndex = 7;
        int reasonColumnIndex = 8;

        Row headerRow = sheet.getRow(findUnitImportDataStartRow(sheet, new DataFormatter()) - 1);
        createCell(headerRow, resultColumnIndex, "Kết quả", resultHeaderStyle);
        createCell(headerRow, reasonColumnIndex, "Lý do lỗi", resultHeaderStyle);

        for (Map.Entry<Integer, String> entry : rowErrors.entrySet()) {
            Row row = sheet.getRow(entry.getKey());
            if (row == null) {
                continue;
            }

            for (int columnIndex = 0; columnIndex <= reasonColumnIndex; columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                }
                cell.setCellStyle(errorCellStyle);
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
            throw new UserMessageException("Không thể tạo file lỗi import đơn vị");
        }
    }

    private CellStyle createImportResultHeaderStyle(Workbook workbook) {
        CellStyle style = createExportHeaderStyle(workbook);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createImportErrorCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private int findUnitImportDataStartRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String codeHeader = normalizeImportText(readCellText(row.getCell(1), formatter));
            String nameHeader = normalizeImportText(readCellText(row.getCell(2), formatter));
            if ("ma don vi".equals(codeHeader) && "ten don vi".equals(nameHeader)) {
                return rowIndex + 1;
            }
        }
        throw new UserMessageException("File import không đúng định dạng: không tìm thấy dòng tiêu đề dữ liệu");
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
        String normalized = normalizeImportText(value);
        return switch (normalized) {
            case "1", "hoat dong", "active" -> 1;
            case "0", "khong hoat dong", "inactive" -> 0;
            default -> throw new UserMessageException("Trạng thái không hợp lệ: " + value);
        };
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
