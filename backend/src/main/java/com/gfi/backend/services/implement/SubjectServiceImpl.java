package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import com.gfi.backend.models.dtos.subject.SubjectCreateRequest;
import com.gfi.backend.models.dtos.subject.SubjectDetailDto;
import com.gfi.backend.models.dtos.subject.SubjectFilterDto;
import com.gfi.backend.models.dtos.subject.SubjectImportResultDto;
import com.gfi.backend.models.dtos.subject.SubjectListItemDto;
import com.gfi.backend.models.dtos.subject.SubjectUpdateRequest;
import com.gfi.backend.models.entities.Subject;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomSubjectRepository;
import com.gfi.backend.repositories.GradeLevelSubjectRepository;
import com.gfi.backend.repositories.SubjectRepository;
import com.gfi.backend.repositories.specifications.SubjectSpecification;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import com.gfi.backend.services.interfaces.SubjectService;
import com.gfi.backend.utils.PageableUtils;
import com.gfi.backend.utils.SecurityUtils;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SubjectRepository subjectRepository;
    private final GradeLevelSubjectRepository gradeLevelSubjectRepository;
    private final ClassroomSubjectRepository classroomSubjectRepository;
    private final SubjectSpecification subjectSpecification;
    private final ImportErrorFileStorageService importErrorFileStorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SubjectListItemDto, SubjectFilterDto> search(PageRequestDto<SubjectFilterDto> request) {
        SubjectFilterDto filter = request.getFilter() == null ? new SubjectFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<Subject> page = subjectRepository.findAll(subjectSpecification.buildSpecification(filter), pageable);
        List<SubjectListItemDto> items = page.getContent().stream().map(this::toListItemDto).toList();

        return PageResponseDto.<SubjectListItemDto, SubjectFilterDto>builder()
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
    public byte[] export(PageRequestDto<SubjectFilterDto> request, ExportType exportType) {
        SubjectFilterDto filter = request == null || request.getFilter() == null ? new SubjectFilterDto() : request.getFilter();
        List<Subject> items = subjectRepository.findAll(subjectSpecification.buildSpecification(filter), Sort.by(Sort.Direction.ASC, "name"));
        return switch (exportType) {
            case EXCEL -> exportSubjectsExcel(items);
            case PDF -> exportSubjectsPdf(items);
        };
    }

    @Override
    public byte[] exportExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("MonHoc");
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);
            CellStyle guideStyle = createGuideStyle(workbook);

            Row titleRow = sheet.createRow(0);
            createCell(titleRow, 0, "IMPORT MÔN HỌC", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row guide1 = sheet.createRow(1);
            createCell(guide1, 0, "Hướng dẫn: Có thể chỉnh sửa trực tiếp file này rồi tải lên để nhập dữ liệu.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            Row guide2 = sheet.createRow(2);
            createCell(guide2, 0, "Các cột bắt buộc: Mã môn học, Tên môn học, Loại môn học, Trạng thái. Loại môn học: Bắt buộc/Tự chọn.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));

            Row guide3 = sheet.createRow(3);
            createCell(guide3, 0, "Nếu mã môn học hoặc tên môn học đã tồn tại, dòng import sẽ bị báo lỗi và không được tạo mới.", guideStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));

            Row headerRow = sheet.createRow(4);
            String[] headers = { "STT", "Mã môn học", "Tên môn học", "Loại môn học", "Trạng thái", "Mô tả" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            Row sampleRow = sheet.createRow(5);
            createCell(sampleRow, 0, 1, bodyStyle);
            createCell(sampleRow, 1, "TOAN", bodyStyle);
            createCell(sampleRow, 2, "Toán", bodyStyle);
            createCell(sampleRow, 3, "Bắt buộc", bodyStyle);
            createCell(sampleRow, 4, "Hoạt động", bodyStyle);
            createCell(sampleRow, 5, "Môn học mẫu", bodyStyle);

            setSubjectSheetWidths(sheet);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel mẫu môn học");
        }
    }

    @Override
    @Transactional
    public SubjectImportResultDto importExcel(MultipartFile file) {
        validateExcelFile(file);
        int successCount = 0;
        Map<Integer, String> rowErrors = new LinkedHashMap<>();
        String errorFileToken = null;
        String errorFileName = null;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int startRow = findSubjectImportDataStartRow(sheet, formatter);

            for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, 1, 5, formatter)) {
                    continue;
                }
                try {
                    upsertSubjectFromRow(row, formatter);
                    successCount++;
                } catch (Exception ex) {
                    rowErrors.put(rowIndex, resolveImportErrorMessage(ex));
                }
            }

            if (!rowErrors.isEmpty()) {
                byte[] errorContent = buildSubjectImportErrorFile(workbook, sheet, rowErrors);
                errorFileName = "subject_import_error_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
                errorFileToken = importErrorFileStorageService.store(
                        errorFileName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        errorContent);
            }
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file import môn học");
        }

        return SubjectImportResultDto.builder()
                .successCount(successCount)
                .failedCount(rowErrors.size())
                .hasErrorFile(!rowErrors.isEmpty())
                .errorFileToken(errorFileToken)
                .errorFileName(errorFileName)
                .build();
    }

    @Override
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupItemDto> getOptions() {
        return subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectDetailDto getById(Long id) {
        return toDetailDto(findSubject(id));
    }

    @Override
    @Transactional
    public SubjectDetailDto create(SubjectCreateRequest request) {
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);

        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        subject.setType(request.getType());
        subject.setDescription(normalizeNullable(request.getDescription()));
        subject.setStatus(request.getStatus());
        subject.setCreatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectDetailDto update(Long id, SubjectUpdateRequest request) {
        Subject subject = findSubject(id);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());

        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);

        subject.setCode(code);
        subject.setName(name);
        subject.setType(request.getType());
        subject.setDescription(normalizeNullable(request.getDescription()));
        subject.setStatus(request.getStatus());
        subject.setUpdatedBy(SecurityUtils.getCurrentUsername());
        return toDetailDto(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = findSubject(id);
        if (gradeLevelSubjectRepository.countBySubjectId(id) > 0 || classroomSubjectRepository.countBySubjectId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SUBJECT_IN_USE);
        }

        subject.setDeletedFlag(1);
        subject.setDeletedAt(LocalDateTime.now());
        subject.setDeletedBy(SecurityUtils.getCurrentUsername());
        subjectRepository.save(subject);
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SUBJECT_NOT_FOUND));
    }

    private void ensureCodeUnique(String code, Long id) {
        subjectRepository.findByCode(code)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_CODE_ALREADY_EXISTS);
                });
    }

    private void ensureNameUnique(String name, Long id) {
        subjectRepository.findByName(name)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_NAME_ALREADY_EXISTS);
                });
    }

    private SubjectDetailDto toDetailDto(Subject subject) {
        return SubjectDetailDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .type(subject.getType())
                .description(subject.getDescription())
                .status(subject.getStatus())
                .build();
    }

    private SubjectListItemDto toListItemDto(Subject subject) {
        return SubjectListItemDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .type(subject.getType())
                .status(subject.getStatus())
                .build();
    }

    private byte[] exportSubjectsExcel(List<Subject> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("MonHoc");
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle infoStyle = createExportInfoStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);

            Row infoRow = sheet.createRow(0);
            createCell(infoRow, 0, buildExportInfoLine(), infoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row titleRow = sheet.createRow(1);
            createCell(titleRow, 0, "DANH SÁCH MÔN HỌC", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            Row headerRow = sheet.createRow(3);
            String[] headers = { "STT", "Mã môn học", "Tên môn học", "Loại môn học", "Trạng thái", "Mô tả" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 4;
            int stt = 1;
            for (Subject item : items) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, stt++, bodyStyle);
                createCell(row, 1, item.getCode(), bodyStyle);
                createCell(row, 2, item.getName(), bodyStyle);
                createCell(row, 3, subjectTypeLabel(item.getType()), bodyStyle);
                createCell(row, 4, statusLabel(item.getStatus()), bodyStyle);
                createCell(row, 5, item.getDescription(), bodyStyle);
            }

            setSubjectSheetWidths(sheet);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel danh sách môn học");
        }
    }

    private byte[] exportSubjectsPdf(List<Subject> items) {
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

            Paragraph title = new Paragraph("DANH SÁCH MÔN HỌC", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.8f, 1.8f, 2.8f, 1.6f, 1.4f, 3.2f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Mã môn học", headerFont);
            addPdfHeaderCell(table, "Tên môn học", headerFont);
            addPdfHeaderCell(table, "Loại", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);
            addPdfHeaderCell(table, "Mô tả", headerFont);

            int stt = 1;
            for (Subject item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, subjectTypeLabel(item.getType()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, statusLabel(item.getStatus()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getDescription(), bodyFont, Element.ALIGN_LEFT);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF danh sách môn học");
        }
    }

    private void upsertSubjectFromRow(Row row, DataFormatter formatter) {
        String code = normalize(readCellText(row.getCell(1), formatter));
        String name = normalize(readCellText(row.getCell(2), formatter));
        Integer type = parseSubjectType(readCellText(row.getCell(3), formatter));
        Integer status = parseStatus(readCellText(row.getCell(4), formatter));
        String description = normalizeNullable(readCellText(row.getCell(5), formatter));

        if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new UserMessageException("Mã môn học và tên môn học là bắt buộc");
        }

        subjectRepository.findByCode(code)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_CODE_ALREADY_EXISTS);
                });

        subjectRepository.findByName(name)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SUBJECT_NAME_ALREADY_EXISTS);
                });

        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        subject.setType(type);
        subject.setStatus(status);
        subject.setDescription(description);
        subject.setCreatedBy(SecurityUtils.getCurrentUsername());
        subject.setDeletedFlag(0);
        subjectRepository.save(subject);
    }

    private byte[] buildSubjectImportErrorFile(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors) {
        CellStyle resultHeaderStyle = createImportResultHeaderStyle(workbook);
        CellStyle errorCellStyle = createImportErrorCellStyle(workbook);
        int resultColumnIndex = 6;
        int reasonColumnIndex = 7;

        Row headerRow = sheet.getRow(findSubjectImportDataStartRow(sheet, new DataFormatter()) - 1);
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
            throw new UserMessageException("Không thể tạo file lỗi import môn học");
        }
    }

    private int findSubjectImportDataStartRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String codeHeader = normalizeImportText(readCellText(row.getCell(1), formatter));
            String nameHeader = normalizeImportText(readCellText(row.getCell(2), formatter));
            if ("ma mon hoc".equals(codeHeader) && "ten mon hoc".equals(nameHeader)) {
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

    private boolean isEmptyRow(Row row, int fromColumn, int toColumn, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = fromColumn; i <= toColumn; i++) {
            if (StringUtils.hasText(readCellText(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String resolveImportErrorMessage(Exception ex) {
        if (ex instanceof UserMessageException userMessageException) {
            return userMessageException.getMessage();
        }
        return ex.getMessage() == null ? "Dữ liệu không hợp lệ" : ex.getMessage();
    }

    private Integer parseSubjectType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new UserMessageException("Loại môn học là bắt buộc");
        }
        String normalized = normalizeImportText(value);
        return switch (normalized) {
            case "0", "bat buoc" -> 0;
            case "1", "tu chon" -> 1;
            default -> throw new UserMessageException("Loại môn học không hợp lệ: " + value);
        };
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
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace('đ', 'd');
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private void setSubjectSheetWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 32 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 18 * 256);
        sheet.setColumnWidth(5, 36 * 256);
    }

    private String subjectTypeLabel(Integer type) {
        return Integer.valueOf(1).equals(type) ? "Tự chọn" : "Bắt buộc";
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "Hoạt động" : "Không hoạt động";
    }

    private String buildExportInfoLine() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
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

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : pageSize;
    }

    private int normalizePageNow(Integer pageNow) {
        return pageNow == null || pageNow <= 0 ? 1 : pageNow;
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
}
