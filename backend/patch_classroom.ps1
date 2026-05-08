$impl_path = "src\main\java\com\gfi\backend\services\implement\ClassroomServiceImpl.java"
$content = Get-Content $impl_path -Raw
if ($content -notmatch "public byte\[\] export") {
    $imports = "
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.classroom.ClassroomImportResultDto;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
"
    $content = $content -replace "import java.util.List;", ("import java.util.List;" + $imports)

    $content = $content -replace "private final DataScopeFilterService dataScopeFilterService;", "private final DataScopeFilterService dataScopeFilterService;`n    private final ImportErrorFileStorageService importErrorFileStorageService;"

    $methods = @"
    private static final String EXPORT_FONT_NAME = `"Times New Roman`";
    private static final String TIMES_FONT_REGULAR_PATH = `"C:/Windows/Fonts/times.ttf`";
    private static final String TIMES_FONT_BOLD_PATH = `"C:/Windows/Fonts/timesbd.ttf`";
    private static final String TIMES_FONT_ITALIC_PATH = `"C:/Windows/Fonts/timesi.ttf`";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern(`"dd/MM/yyyy HH:mm`");

    @Override
    @Transactional(readOnly = true)
    public byte[] export(PageRequestDto<ClassroomFilterDto> request, ExportType exportType) {
        ClassroomFilterDto filter = request.getFilter() == null ? new ClassroomFilterDto() : request.getFilter();
        List<ResolvedScope> resolvedScopes = dataScopeFilterService.getResolvedScopes(FEATURE, ActionType.VIEW);
        List<Classroom> items = classroomRepository.findAll(
                classroomSpecification.buildSpecification(filter, resolvedScopes),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, `"name`").and(org.springframework.data.domain.Sort.by(`"id`")));

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
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(`".xlsx`")) {
            throw new UserMessageException(`"File import phải là file Excel .xlsx`");
        }
        
        int successCount = 0;
        int failedCount = 0;
        Map<Integer, String> rowErrors = new LinkedHashMap<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet(`"LopHoc`");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new UserMessageException(`"File Excel không có sheet LopHoc`");
            }

            DataFormatter formatter = new DataFormatter();
            int dataStartRowIndex = findClassroomImportDataStartRow(sheet, formatter);

            for (int rowIndex = dataStartRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
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

                if (!StringUtils.hasText(code) || !StringUtils.hasText(name) || !StringUtils.hasText(unitCode) || !StringUtils.hasText(gradeCode) || !StringUtils.hasText(schoolYearName)) {
                    failedCount++;
                    rowErrors.put(rowIndex, `"Mã lớp, tên lớp, mã đơn vị, mã khối, tên năm học là bắt buộc`");
                    continue;
                }

                String normalizedCode = code.trim();
                
                try {
                    Unit unit = unitRepository.findByCode(unitCode.trim()).orElseThrow(() -> new UserMessageException(`"Đơn vị không tồn tại`"));
                    GradeLevel gradeLevel = gradeLevelRepository.findByCode(gradeCode.trim()).orElseThrow(() -> new UserMessageException(`"Khối không tồn tại`"));
                    SchoolYear schoolYear = schoolYearRepository.findByName(schoolYearName.trim()).orElseThrow(() -> new UserMessageException(`"Năm học không tồn tại`"));
                    
                    validateClassroomTargetScope(ActionType.ADD, unit, gradeLevel);
                    validateUnique(unit.getId(), gradeLevel.getId(), schoolYear.getId(), normalizedCode, name.trim(), null);

                    Classroom classroom = new Classroom();
                    classroom.setCode(normalizedCode);
                    classroom.setName(name.trim());
                    classroom.setUnit(unit);
                    classroom.setGradeLevel(gradeLevel);
                    classroom.setSchoolYear(schoolYear);
                    
                    String normStatus = normalizeImportText(statusText);
                    Integer status = 1;
                    if (`"0`".equals(normStatus) || `"khong hoat dong`".equals(normStatus)) {
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
                    String msg = ex instanceof UserMessageException ? ex.getMessage() : `"Dữ liệu không hợp lệ`";
                    rowErrors.put(rowIndex, msg);
                }
            }

            String errorFileToken = null;
            String errorFileName = null;
            if (!rowErrors.isEmpty()) {
                byte[] errorFileContent = buildClassroomImportErrorFile(workbook, sheet, rowErrors);
                errorFileName = `"classroom_import_error_`" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(`"yyyyMMddHHmmss`")) + `".xlsx`";
                errorFileToken = importErrorFileStorageService.store(
                        errorFileName,
                        `"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`",
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
            throw new UserMessageException(`"Không thể đọc file Excel lớp học`");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }
    
    private String readCellText(org.apache.poi.ss.usermodel.Cell cell, DataFormatter formatter) {
        return cell == null ? `"`" : formatter.formatCellValue(cell).trim();
    }
    
    private String normalizeImportText(String value) {
        if (!StringUtils.hasText(value)) {
            return `"`";
        }
        String normalized = java.text.Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll(`"\\p{M}+`", `"`");
        normalized = normalized.replace('đ', 'd');
        return normalized.replaceAll(`"\\s+`", `" `").trim();
    }

    private int findClassroomImportDataStartRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String codeHeader = normalizeImportText(readCellText(row.getCell(1), formatter));
            String nameHeader = normalizeImportText(readCellText(row.getCell(2), formatter));
            if (`"ma lop`".equals(codeHeader) && `"ten lop`".equals(nameHeader)) {
                return rowIndex + 1;
            }
        }
        throw new UserMessageException(`"File import không đúng định dạng: không tìm thấy dòng tiêu đề dữ liệu`");
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
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) headerRow = sheet.createRow(headerRowIndex);
        org.apache.poi.ss.usermodel.Cell c1 = headerRow.createCell(resultColumnIndex); c1.setCellValue(`"Kết quả`"); c1.setCellStyle(resultHeaderStyle);
        org.apache.poi.ss.usermodel.Cell c2 = headerRow.createCell(reasonColumnIndex); c2.setCellValue(`"Lý do lỗi`"); c2.setCellStyle(resultHeaderStyle);

        for (Map.Entry<Integer, String> entry : rowErrors.entrySet()) {
            Row row = sheet.getRow(entry.getKey());
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
            row.getCell(resultColumnIndex).setCellValue(`"Thất bại`");
            row.getCell(reasonColumnIndex).setCellValue(entry.getValue());
        }

        sheet.setColumnWidth(resultColumnIndex, 16 * 256);
        sheet.setColumnWidth(reasonColumnIndex, 42 * 256);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException(`"Không thể tạo file lỗi import`");
        }
    }

    private byte[] exportClassroomsExcel(List<Classroom> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(`"LopHoc`");
            
            Row infoRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(`"Thời gian tải: `" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + `" | Người tải: `" + SecurityUtils.getCurrentUsername());
            CellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setAlignment(HorizontalAlignment.RIGHT);
            infoStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font infoFont = workbook.createFont();
            infoFont.setFontName(EXPORT_FONT_NAME);
            infoStyle.setFont(infoFont);
            infoCell.setCellStyle(infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            
            Row titleRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(`"DANH SÁCH LỚP HỌC`");
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

            Row headerRow = sheet.createRow(3);
            String[] headers = { `"STT`", `"Mã lớp`", `"Tên lớp`", `"Đơn vị`", `"Khối`", `"Năm học`", `"Mô tả`", `"Trạng thái`" };
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
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                org.apache.poi.ss.usermodel.Cell c = row.createCell(col++); c.setCellValue(stt++); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getCode()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getName()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getUnit() != null ? item.getUnit().getName() : `"`"); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getGradeLevel() != null ? item.getGradeLevel().getName() : `"`"); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getSchoolYear() != null ? item.getSchoolYear().getName() : `"`"); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(item.getDescription()); c.setCellStyle(bodyStyle);
                c = row.createCell(col++); c.setCellValue(Integer.valueOf(1).equals(item.getStatus()) ? `"Hoạt động`" : `"Không hoạt động`"); c.setCellStyle(bodyStyle);
            }
            
            for(int i=0; i<8; i++) sheet.autoSizeColumn(i);
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException(`"Không thể tạo file Excel`");
        }
    }

    private byte[] exportClassroomsExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(`"LopHoc`");
            
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(`"IMPORT LỚP HỌC`");
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
            guideStyle.setFont(guideFont);
            
            Row guide1 = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell gc1 = guide1.createCell(0); gc1.setCellValue(`"Hướng dẫn: Điền mã lớp, tên lớp, mã đơn vị, mã khối, tên năm học. Trạng thái: Hoạt động / Không hoạt động.`"); gc1.setCellStyle(guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
            
            Row headerRow = sheet.createRow(3);
            String[] headers = { `"STT`", `"Mã lớp`", `"Tên lớp`", `"Mã đơn vị`", `"Mã khối`", `"Tên năm học`", `"Trạng thái`" };
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

            Row sampleRow = sheet.createRow(4);
            int col = 0;
            org.apache.poi.ss.usermodel.Cell c = sampleRow.createCell(col++); c.setCellValue(`"1`"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue(`"THVN050_5B_1`"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue(`"Lớp 5B`"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue(`"THVN050`"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue(`"KHOI_5`"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue(`"Năm học 2025 - 2026`"); c.setCellStyle(bodyStyle);
            c = sampleRow.createCell(col++); c.setCellValue(`"Hoạt động`"); c.setCellStyle(bodyStyle);
            
            for(int i=0; i<7; i++) sheet.autoSizeColumn(i);
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException(`"Không thể tạo file template`");
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

            Paragraph exportInfo = new Paragraph(`"Thời gian tải: `" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + `" | Người tải: `" + SecurityUtils.getCurrentUsername(), infoFont);
            exportInfo.setAlignment(Element.ALIGN_RIGHT);
            exportInfo.setSpacingAfter(6f);
            document.add(exportInfo);

            Paragraph title = new Paragraph(`"DANH SÁCH LỚP HỌC`", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.8f, 1.8f, 2.6f, 3.0f, 1.5f, 2.5f, 1.5f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, `"STT`", headerFont);
            addPdfHeaderCell(table, `"Mã lớp`", headerFont);
            addPdfHeaderCell(table, `"Tên lớp`", headerFont);
            addPdfHeaderCell(table, `"Đơn vị`", headerFont);
            addPdfHeaderCell(table, `"Khối`", headerFont);
            addPdfHeaderCell(table, `"Năm học`", headerFont);
            addPdfHeaderCell(table, `"Trạng thái`", headerFont);

            int stt = 1;
            for (Classroom item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getUnit() != null ? item.getUnit().getName() : `"`", bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getGradeLevel() != null ? item.getGradeLevel().getName() : `"`", bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getSchoolYear() != null ? item.getSchoolYear().getName() : `"`", bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, Integer.valueOf(1).equals(item.getStatus()) ? `"Hoạt động`" : `"Không hoạt động`", bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException(`"Không thể tạo file PDF`");
        }
    }

    private void addPdfHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? `"`" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBackgroundColor(new java.awt.Color(224, 242, 241));
        table.addCell(cell);
    }

    private void addPdfBodyCell(PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? `"`" : text, font));
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
"@
    
    $content = $content -replace "\}\s*$", ($methods + "`n}")
    Set-Content -Path $impl_path -Value $content -Encoding UTF8
}

$controller_path = "src\main\java\com\gfi\backend\controllers\ClassroomController.java"
$content = Get-Content $controller_path -Raw

if ($content -notmatch "export\(") {
    $imports = "
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import org.springframework.web.multipart.MultipartFile;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.dtos.classroom.ClassroomImportResultDto;
import com.gfi.backend.models.enums.ExportType;
"
    $content = $content -replace "import java.util.List;", ("import java.util.List;" + $imports)

    $endpoints = @"
    @PostMapping("/export")
    @DataScoped(feature = "CLASS_MANAGEMENT", action = ActionType.DOWNLOAD)
    @Operation(summary = "Xuất danh sách lớp", description = "Xuất danh sách lớp theo điều kiện tìm kiếm.")
    public ResponseEntity<byte[]> export(
            @RequestBody(required = false) PageRequestDto<ClassroomFilterDto> request,
            @RequestParam(defaultValue = "EXCEL") ExportType exportType) {
        PageRequestDto<ClassroomFilterDto> safeRequest = request == null ? new PageRequestDto<>() : request;
        byte[] content = classroomService.export(safeRequest, exportType);
        String extension = exportType == ExportType.PDF ? "pdf" : "xlsx";
        String contentType = exportType == ExportType.PDF
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = "danh-sach-lop-hoc." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }

    @GetMapping("/export-template")
    @DataScoped(feature = "CLASS_MANAGEMENT", action = ActionType.DOWNLOAD)
    @Operation(summary = "Tải file Excel mẫu", description = "Tải file Excel mẫu để import danh sách lớp.")
    public ResponseEntity<byte[]> exportExcelTemplate() {
        byte[] content = classroomService.exportExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-import-lop-hoc.xlsx", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/import")
    @DataScoped(feature = "CLASS_MANAGEMENT", action = ActionType.ADD)
    @Operation(summary = "Import lớp học từ Excel", description = "Import danh sách lớp học từ file Excel.")
    public ResponseEntity<ApiResult<ClassroomImportResultDto>> importExcel(@RequestParam("file") MultipartFile file) {
        return executeApiResult(() -> ApiResult.success(classroomService.importExcel(file), "Import lớp học hoàn tất"));
    }

    @GetMapping("/import-errors/{token}")
    @DataScoped(feature = "CLASS_MANAGEMENT", action = ActionType.VIEW)
    @Operation(summary = "Tải file lỗi import", description = "Tải file Excel chứa các dòng dữ liệu lỗi sau khi import.")
    public ResponseEntity<byte[]> getImportErrorFile(@PathVariable String token) {
        TemporaryFileDto fileDto = classroomService.getImportErrorFile(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileDto.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(fileDto.getContentType()))
                .body(fileDto.getContent());
    }
"@
    $content = $content -replace "\}\s*$", ($endpoints + "`n}")
    Set-Content -Path $controller_path -Value $content -Encoding UTF8
}
