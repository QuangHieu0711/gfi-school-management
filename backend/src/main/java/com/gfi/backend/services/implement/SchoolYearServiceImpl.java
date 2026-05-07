package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
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
import org.apache.poi.ss.usermodel.DateUtil;
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
import com.gfi.backend.models.dtos.schoolyear.SchoolYearCreateRequest;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearFilterDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearImportResultDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearItemDto;
import com.gfi.backend.models.dtos.schoolyear.SchoolYearUpdateRequest;
import com.gfi.backend.models.entities.SchoolYear;
import com.gfi.backend.models.entities.Semester;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.SchoolYearRepository;
import com.gfi.backend.repositories.SemesterRepository;
import com.gfi.backend.repositories.specifications.SchoolYearSpecification;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
import com.gfi.backend.services.interfaces.SchoolYearService;
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
public class SchoolYearServiceImpl implements SchoolYearService {

    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;
    private final ClassroomRepository classroomRepository;
    private final SchoolYearSpecification schoolYearSpecification;
    private final ImportErrorFileStorageService importErrorFileStorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SchoolYearItemDto, SchoolYearFilterDto> search(PageRequestDto<SchoolYearFilterDto> request) {
        SchoolYearFilterDto filter = request.getFilter() == null ? new SchoolYearFilterDto() : request.getFilter();
        int pageSize = normalizePageSize(request.getPageSize());
        int pageNow = normalizePageNow(request.getPageNow());
        Pageable pageable = PageableUtils.newestFirst(pageNow, pageSize);

        Page<SchoolYear> page = schoolYearRepository.findAll(schoolYearSpecification.buildSpecification(filter), pageable);
        List<SchoolYearItemDto> items = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<SchoolYearItemDto, SchoolYearFilterDto>builder()
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
    public byte[] export(PageRequestDto<SchoolYearFilterDto> request, ExportType exportType) {
        SchoolYearFilterDto filter = request == null || request.getFilter() == null ? new SchoolYearFilterDto() : request.getFilter();
        List<SchoolYear> items = schoolYearRepository.findAll(schoolYearSpecification.buildSpecification(filter),
                Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return switch (exportType) {
            case EXCEL -> exportSchoolYearsExcel(items);
            case PDF -> exportSchoolYearsPdf(items);
        };
    }

    @Override
    public byte[] exportExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);
            CellStyle guideStyle = createGuideStyle(workbook);

            Sheet schoolYearSheet = workbook.createSheet("NamHoc");
            Row titleRow = schoolYearSheet.createRow(0);
            createCell(titleRow, 0, "CẤU HÌNH NĂM HỌC", titleStyle);
            schoolYearSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            Row guide1 = schoolYearSheet.createRow(1);
            createCell(guide1, 0, "Sheet này dùng để khai báo năm học. Nếu mã hoặc tên năm học đã tồn tại, dòng import sẽ bị báo lỗi.", guideStyle);
            schoolYearSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            Row headerRow = schoolYearSheet.createRow(3);
            String[] headers = { "STT", "Mã năm học", "Tên năm học", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái", "Hiện hành", "Mô tả" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }
            Row sampleRow = schoolYearSheet.createRow(4);
            createCell(sampleRow, 0, 1, bodyStyle);
            createCell(sampleRow, 1, "NH2025", bodyStyle);
            createCell(sampleRow, 2, "Năm học 2025-2026", bodyStyle);
            createCell(sampleRow, 3, "2025-09-01", bodyStyle);
            createCell(sampleRow, 4, "2026-05-31", bodyStyle);
            createCell(sampleRow, 5, "Hoạt động", bodyStyle);
            createCell(sampleRow, 6, "Có", bodyStyle);
            createCell(sampleRow, 7, "Năm học mẫu", bodyStyle);
            setSchoolYearSheetWidths(schoolYearSheet);

            Sheet semesterSheet = workbook.createSheet("HocKy");
            Row semesterTitleRow = semesterSheet.createRow(0);
            createCell(semesterTitleRow, 0, "CẤU HÌNH HỌC KỲ", titleStyle);
            semesterSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            Row semesterGuideRow = semesterSheet.createRow(1);
            createCell(semesterGuideRow, 0, "Sheet này dùng để khai báo học kỳ theo mã năm học ở sheet NamHoc.", guideStyle);
            semesterSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            Row semesterHeaderRow = semesterSheet.createRow(3);
            String[] semesterHeaders = { "STT", "Mã năm học", "Mã học kỳ", "Tên học kỳ", "Thứ tự học kỳ", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái", "Hiện hành", "Mô tả" };
            for (int i = 0; i < semesterHeaders.length; i++) {
                createCell(semesterHeaderRow, i, semesterHeaders[i], headerStyle);
            }

            Row semesterSampleRow1 = semesterSheet.createRow(4);
            createCell(semesterSampleRow1, 0, 1, bodyStyle);
            createCell(semesterSampleRow1, 1, "NH2025", bodyStyle);
            createCell(semesterSampleRow1, 2, "HK1", bodyStyle);
            createCell(semesterSampleRow1, 3, "Học kỳ 1", bodyStyle);
            createCell(semesterSampleRow1, 4, 1, bodyStyle);
            createCell(semesterSampleRow1, 5, "2025-09-01", bodyStyle);
            createCell(semesterSampleRow1, 6, "2026-01-15", bodyStyle);
            createCell(semesterSampleRow1, 7, "Hoạt động", bodyStyle);
            createCell(semesterSampleRow1, 8, "Có", bodyStyle);
            createCell(semesterSampleRow1, 9, "Học kỳ mẫu 1", bodyStyle);

            Row semesterSampleRow2 = semesterSheet.createRow(5);
            createCell(semesterSampleRow2, 0, 2, bodyStyle);
            createCell(semesterSampleRow2, 1, "NH2025", bodyStyle);
            createCell(semesterSampleRow2, 2, "HK2", bodyStyle);
            createCell(semesterSampleRow2, 3, "Học kỳ 2", bodyStyle);
            createCell(semesterSampleRow2, 4, 2, bodyStyle);
            createCell(semesterSampleRow2, 5, "2026-01-16", bodyStyle);
            createCell(semesterSampleRow2, 6, "2026-05-31", bodyStyle);
            createCell(semesterSampleRow2, 7, "Hoạt động", bodyStyle);
            createCell(semesterSampleRow2, 8, "Không", bodyStyle);
            createCell(semesterSampleRow2, 9, "Học kỳ mẫu 2", bodyStyle);
            setSemesterSheetWidths(semesterSheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel mẫu năm học");
        }
    }

    @Override
    @Transactional
    public SchoolYearImportResultDto importExcel(MultipartFile file) {
        validateExcelFile(file);
        int successCount = 0;
        Map<String, SchoolYear> schoolYearsByCode = new LinkedHashMap<>();
        Map<Integer, String> schoolYearErrors = new LinkedHashMap<>();
        Map<Integer, String> semesterErrors = new LinkedHashMap<>();
        String errorFileToken = null;
        String errorFileName = null;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet schoolYearSheet = workbook.getSheet("NamHoc");
            Sheet semesterSheet = workbook.getSheet("HocKy");
            if (schoolYearSheet == null || semesterSheet == null) {
                throw new UserMessageException("File import phải có đủ 2 sheet NamHoc và HocKy");
            }

            DataFormatter formatter = new DataFormatter();
            int schoolYearStartRow = findSchoolYearImportDataStartRow(schoolYearSheet, formatter);
            int semesterStartRow = findSemesterImportDataStartRow(semesterSheet, formatter);

            for (int rowIndex = schoolYearStartRow; rowIndex <= schoolYearSheet.getLastRowNum(); rowIndex++) {
                Row row = schoolYearSheet.getRow(rowIndex);
                if (isEmptyRow(row, 1, 7, formatter)) {
                    continue;
                }
                try {
                    SchoolYear schoolYear = createSchoolYearFromRow(row, formatter);
                    schoolYearsByCode.put(schoolYear.getCode(), schoolYear);
                    successCount++;
                } catch (Exception ex) {
                    schoolYearErrors.put(rowIndex, resolveImportErrorMessage(ex));
                }
            }

            for (int rowIndex = semesterStartRow; rowIndex <= semesterSheet.getLastRowNum(); rowIndex++) {
                Row row = semesterSheet.getRow(rowIndex);
                if (isEmptyRow(row, 1, 9, formatter)) {
                    continue;
                }
                try {
                    createSemesterFromRow(row, formatter, schoolYearsByCode);
                    successCount++;
                } catch (Exception ex) {
                    semesterErrors.put(rowIndex, resolveImportErrorMessage(ex));
                }
            }

            if (!schoolYearErrors.isEmpty() || !semesterErrors.isEmpty()) {
                byte[] errorContent = buildSchoolYearImportErrorFile(workbook, schoolYearSheet, semesterSheet, schoolYearErrors, semesterErrors);
                errorFileName = "school_year_import_error_"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
                errorFileToken = importErrorFileStorageService.store(
                        errorFileName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        errorContent);
            }
        } catch (IOException ex) {
            throw new UserMessageException("Không thể đọc file import năm học");
        }

        return SchoolYearImportResultDto.builder()
                .successCount(successCount)
                .failedCount(schoolYearErrors.size() + semesterErrors.size())
                .hasErrorFile(!schoolYearErrors.isEmpty() || !semesterErrors.isEmpty())
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
        return schoolYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(item -> LookupItemDto.builder().id(item.getId()).name(item.getName()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolYearItemDto getById(Long id) {
        return toDto(findSchoolYear(id));
    }

    @Override
    @Transactional(readOnly = true)
    public LookupItemDto getCurrentSchoolYear() {
        return schoolYearRepository.findByIsCurrentTrueAndDeletedFlagEquals(0)
                .map(sy -> LookupItemDto.builder().id(sy.getId()).name(sy.getName()).build())
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    @Override
    @Transactional
    public SchoolYearItemDto create(SchoolYearCreateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateNoOverlappingSchoolYear(request.getStartDate(), request.getEndDate(), null);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);

        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setCode(code);
        schoolYear.setName(name);
        schoolYear.setStartDate(request.getStartDate());
        schoolYear.setEndDate(request.getEndDate());
        schoolYear.setStatus(request.getStatus());
        schoolYear.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        schoolYear.setDescription(normalizeNullable(request.getDescription()));
        schoolYear.setCreatedBy(getCurrentUsername());

        SchoolYear saved = schoolYearRepository.save(schoolYear);
        applyCurrentFlag(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public SchoolYearItemDto update(Long id, SchoolYearUpdateRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateNoOverlappingSchoolYear(request.getStartDate(), request.getEndDate(), id);
        SchoolYear schoolYear = findSchoolYear(id);
        String code = normalize(request.getCode());
        String name = normalize(request.getName());
        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);

        schoolYear.setCode(code);
        schoolYear.setName(name);
        schoolYear.setStartDate(request.getStartDate());
        schoolYear.setEndDate(request.getEndDate());
        schoolYear.setStatus(request.getStatus());
        schoolYear.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        schoolYear.setDescription(normalizeNullable(request.getDescription()));
        schoolYear.setUpdatedBy(getCurrentUsername());

        SchoolYear saved = schoolYearRepository.save(schoolYear);
        applyCurrentFlag(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SchoolYear schoolYear = findSchoolYear(id);
        if (semesterRepository.countBySchoolYearId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_IN_USE);
        }
        if (classroomRepository.countBySchoolYearId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_IN_USE);
        }

        schoolYear.setDeletedFlag(1);
        schoolYear.setDeletedAt(LocalDateTime.now());
        schoolYear.setDeletedBy(SecurityUtils.getCurrentUsername());
        schoolYearRepository.save(schoolYear);
    }

    private byte[] exportSchoolYearsExcel(List<SchoolYear> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle titleStyle = createExportTitleStyle(workbook);
            CellStyle infoStyle = createExportInfoStyle(workbook);
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            CellStyle bodyStyle = createExportBodyStyle(workbook);

            Sheet schoolYearSheet = workbook.createSheet("NamHoc");
            Row infoRow = schoolYearSheet.createRow(0);
            createCell(infoRow, 0, buildExportInfoLine(), infoStyle);
            schoolYearSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            Row titleRow = schoolYearSheet.createRow(1);
            createCell(titleRow, 0, "CẤU HÌNH NĂM HỌC", titleStyle);
            schoolYearSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
            Row headerRow = schoolYearSheet.createRow(3);
            String[] headers = { "STT", "Mã năm học", "Tên năm học", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái", "Hiện hành", "Mô tả" };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 4;
            int stt = 1;
            for (SchoolYear item : items) {
                Row row = schoolYearSheet.createRow(rowIndex++);
                createCell(row, 0, stt++, bodyStyle);
                createCell(row, 1, item.getCode(), bodyStyle);
                createCell(row, 2, item.getName(), bodyStyle);
                createCell(row, 3, formatDate(item.getStartDate()), bodyStyle);
                createCell(row, 4, formatDate(item.getEndDate()), bodyStyle);
                createCell(row, 5, statusLabel(item.getStatus()), bodyStyle);
                createCell(row, 6, booleanLabel(item.getIsCurrent()), bodyStyle);
                createCell(row, 7, item.getDescription(), bodyStyle);
            }
            setSchoolYearSheetWidths(schoolYearSheet);

            Sheet semesterSheet = workbook.createSheet("HocKy");
            Row semesterTitleRow = semesterSheet.createRow(0);
            createCell(semesterTitleRow, 0, "CẤU HÌNH HỌC KỲ", titleStyle);
            semesterSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
            Row semesterHeaderRow = semesterSheet.createRow(3);
            String[] semesterHeaders = { "STT", "Mã năm học", "Mã học kỳ", "Tên học kỳ", "Thứ tự học kỳ", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái", "Hiện hành", "Mô tả" };
            for (int i = 0; i < semesterHeaders.length; i++) {
                createCell(semesterHeaderRow, i, semesterHeaders[i], headerStyle);
            }

            int semesterRowIndex = 4;
            int semesterStt = 1;
            for (SchoolYear item : items) {
                List<Semester> semesters = semesterRepository.findBySchoolYearId(item.getId()).stream()
                        .filter(semester -> semester.getDeletedFlag() == null || semester.getDeletedFlag() == 0)
                        .sorted((a, b) -> Integer.compare(
                                a.getSemesterOrder() == null ? 0 : a.getSemesterOrder(),
                                b.getSemesterOrder() == null ? 0 : b.getSemesterOrder()))
                        .toList();
                for (Semester semester : semesters) {
                    Row row = semesterSheet.createRow(semesterRowIndex++);
                    createCell(row, 0, semesterStt++, bodyStyle);
                    createCell(row, 1, item.getCode(), bodyStyle);
                    createCell(row, 2, semester.getCode(), bodyStyle);
                    createCell(row, 3, semester.getName(), bodyStyle);
                    createCell(row, 4, semester.getSemesterOrder(), bodyStyle);
                    createCell(row, 5, formatDate(semester.getStartDate()), bodyStyle);
                    createCell(row, 6, formatDate(semester.getEndDate()), bodyStyle);
                    createCell(row, 7, statusLabel(semester.getStatus()), bodyStyle);
                    createCell(row, 8, booleanLabel(semester.getIsCurrent()), bodyStyle);
                    createCell(row, 9, semester.getDescription(), bodyStyle);
                }
            }
            setSemesterSheetWidths(semesterSheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel cấu hình năm học");
        }
    }

    private byte[] exportSchoolYearsPdf(List<SchoolYear> items) {
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

            Paragraph title = new Paragraph("CẤU HÌNH NĂM HỌC", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 0.8f, 1.8f, 2.8f, 1.8f, 1.8f, 1.4f, 1.2f, 2.6f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "STT", headerFont);
            addPdfHeaderCell(table, "Mã năm học", headerFont);
            addPdfHeaderCell(table, "Tên năm học", headerFont);
            addPdfHeaderCell(table, "Ngày bắt đầu", headerFont);
            addPdfHeaderCell(table, "Ngày kết thúc", headerFont);
            addPdfHeaderCell(table, "Trạng thái", headerFont);
            addPdfHeaderCell(table, "Hiện hành", headerFont);
            addPdfHeaderCell(table, "Mô tả", headerFont);

            int stt = 1;
            for (SchoolYear item : items) {
                addPdfBodyCell(table, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, formatDate(item.getStartDate()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, formatDate(item.getEndDate()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, statusLabel(item.getStatus()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, booleanLabel(item.getIsCurrent()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getDescription(), bodyFont, Element.ALIGN_LEFT);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF cấu hình năm học");
        }
    }

    private SchoolYear createSchoolYearFromRow(Row row, DataFormatter formatter) {
        String code = normalize(readCellText(row.getCell(1), formatter));
        String name = normalize(readCellText(row.getCell(2), formatter));
        LocalDate startDate = parseDate(row.getCell(3), formatter, "Ngày bắt đầu năm học");
        LocalDate endDate = parseDate(row.getCell(4), formatter, "Ngày kết thúc năm học");
        Integer status = parseStatus(readCellText(row.getCell(5), formatter));
        Boolean isCurrent = parseBooleanFlag(readCellText(row.getCell(6), formatter));
        String description = normalizeNullable(readCellText(row.getCell(7), formatter));

        if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new UserMessageException("Mã năm học và tên năm học là bắt buộc");
        }
        validateDateRange(startDate, endDate);

        schoolYearRepository.findByCode(code)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_CODE_ALREADY_EXISTS);
                });

        schoolYearRepository.findByName(name)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NAME_ALREADY_EXISTS);
                });

        validateNoOverlappingSchoolYear(startDate, endDate, null);

        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setCode(code);
        schoolYear.setName(name);
        schoolYear.setStartDate(startDate);
        schoolYear.setEndDate(endDate);
        schoolYear.setStatus(status);
        schoolYear.setIsCurrent(Boolean.TRUE.equals(isCurrent));
        schoolYear.setDescription(description);
        schoolYear.setCreatedBy(getCurrentUsername());
        schoolYear.setDeletedFlag(0);
        SchoolYear saved = schoolYearRepository.save(schoolYear);
        applyCurrentFlag(saved);
        return saved;
    }

    private void createSemesterFromRow(Row row, DataFormatter formatter, Map<String, SchoolYear> schoolYearsByCode) {
        String schoolYearCode = normalize(readCellText(row.getCell(1), formatter));
        String code = normalize(readCellText(row.getCell(2), formatter));
        String name = normalize(readCellText(row.getCell(3), formatter));
        Integer semesterOrder = parseInteger(readCellText(row.getCell(4), formatter), "Thứ tự học kỳ");
        LocalDate startDate = parseDate(row.getCell(5), formatter, "Ngày bắt đầu học kỳ");
        LocalDate endDate = parseDate(row.getCell(6), formatter, "Ngày kết thúc học kỳ");
        Integer status = parseStatus(readCellText(row.getCell(7), formatter));
        Boolean isCurrent = parseBooleanFlag(readCellText(row.getCell(8), formatter));
        String description = normalizeNullable(readCellText(row.getCell(9), formatter));

        if (!StringUtils.hasText(schoolYearCode) || !StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new UserMessageException("Mã năm học, mã học kỳ và tên học kỳ là bắt buộc");
        }

        SchoolYear schoolYear = schoolYearsByCode.get(schoolYearCode);
        if (schoolYear == null) {
            schoolYear = schoolYearRepository.findByCode(schoolYearCode)
                    .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                    .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
        }

        validateDateRange(startDate, endDate);
        validateSemesterWithinSchoolYearDates(startDate, endDate, schoolYear);

        semesterRepository.findBySchoolYearIdAndCode(schoolYear.getId(), code)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_CODE_ALREADY_EXISTS);
                });

        validateSemesterUnique(schoolYear.getId(), code, name, semesterOrder, null);
        validateNoOverlappingSemester(schoolYear.getId(), startDate, endDate, null);

        Semester semester = new Semester();
        semester.setSchoolYear(schoolYear);
        semester.setCode(code);
        semester.setName(name);
        semester.setSemesterOrder(semesterOrder);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setStatus(status);
        semester.setIsCurrent(Boolean.TRUE.equals(isCurrent));
        semester.setDescription(description);
        semester.setCreatedBy(getCurrentUsername());
        semester.setDeletedFlag(0);
        Semester saved = semesterRepository.save(semester);
        applySemesterCurrentFlag(saved);
    }

    private byte[] buildSchoolYearImportErrorFile(Workbook workbook, Sheet schoolYearSheet, Sheet semesterSheet,
            Map<Integer, String> schoolYearErrors, Map<Integer, String> semesterErrors) {
        annotateImportErrors(workbook, schoolYearSheet, schoolYearErrors, 8, 9, this::findSchoolYearImportDataStartRow);
        annotateImportErrors(workbook, semesterSheet, semesterErrors, 10, 11, this::findSemesterImportDataStartRow);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file lỗi import năm học");
        }
    }

    private void annotateImportErrors(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors, int resultColumnIndex,
            int reasonColumnIndex, SheetStartRowResolver resolver) {
        if (rowErrors.isEmpty()) {
            return;
        }
        CellStyle resultHeaderStyle = createImportResultHeaderStyle(workbook);
        CellStyle errorCellStyle = createImportErrorCellStyle(workbook);

        int headerRowIndex = resolver.resolve(sheet, new DataFormatter()) - 1;
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            headerRow = sheet.createRow(headerRowIndex);
        }
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
                    cell.setCellStyle(errorCellStyle);
                    continue;
                }
                cell.setCellStyle(createHighlightedImportCellStyle(workbook, cell.getCellStyle()));
            }
            row.getCell(resultColumnIndex).setCellValue("Thất bại");
            row.getCell(reasonColumnIndex).setCellValue(entry.getValue());
        }
        sheet.setColumnWidth(resultColumnIndex, 16 * 256);
        sheet.setColumnWidth(reasonColumnIndex, 42 * 256);
    }

    private int findSchoolYearImportDataStartRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String codeHeader = normalizeImportText(readCellText(row.getCell(1), formatter));
            String nameHeader = normalizeImportText(readCellText(row.getCell(2), formatter));
            if ("ma nam hoc".equals(codeHeader) && "ten nam hoc".equals(nameHeader)) {
                return rowIndex + 1;
            }
        }
        throw new UserMessageException("File import không đúng định dạng: không tìm thấy dòng tiêu đề dữ liệu của sheet NamHoc");
    }

    private int findSemesterImportDataStartRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String schoolYearCodeHeader = normalizeImportText(readCellText(row.getCell(1), formatter));
            String codeHeader = normalizeImportText(readCellText(row.getCell(2), formatter));
            if ("ma nam hoc".equals(schoolYearCodeHeader) && "ma hoc ky".equals(codeHeader)) {
                return rowIndex + 1;
            }
        }
        throw new UserMessageException("File import không đúng định dạng: không tìm thấy dòng tiêu đề dữ liệu của sheet HocKy");
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

    private Boolean parseBooleanFlag(String value) {
        if (!StringUtils.hasText(value)) {
            return Boolean.FALSE;
        }
        String normalized = normalizeImportText(value);
        return switch (normalized) {
            case "1", "co", "true", "yes" -> Boolean.TRUE;
            case "0", "khong", "false", "no" -> Boolean.FALSE;
            default -> throw new UserMessageException("Giá trị cờ không hợp lệ: " + value);
        };
    }

    private Integer parseInteger(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new UserMessageException(fieldName + " là bắt buộc");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new UserMessageException(fieldName + " không hợp lệ");
        }
    }

    private LocalDate parseDate(Cell cell, DataFormatter formatter, String fieldName) {
        String value = readCellText(cell, formatter);
        if (!StringUtils.hasText(value)) {
            throw new UserMessageException(fieldName + " là bắt buộc");
        }
        if (cell != null) {
            try {
                if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate();
                }
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
        }
        List<DateTimeFormatter> acceptedFormats = List.of(
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        for (DateTimeFormatter dateFormatter : acceptedFormats) {
            try {
                return LocalDate.parse(value.trim(), dateFormatter);
            } catch (Exception ignored) {
            }
        }
        throw new UserMessageException(fieldName + " không đúng định dạng");
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

    private void ensureCodeUnique(String code, Long id) {
        schoolYearRepository.findByCode(code)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_CODE_ALREADY_EXISTS);
                });
    }

    private void ensureNameUnique(String name, Long id) {
        schoolYearRepository.findByName(name)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NAME_ALREADY_EXISTS);
                });
    }

    private void applyCurrentFlag(SchoolYear schoolYear) {
        if (Boolean.TRUE.equals(schoolYear.getIsCurrent())) {
            schoolYearRepository.clearCurrentExcept(schoolYear.getId());
        }
    }

    private void applySemesterCurrentFlag(Semester semester) {
        if (Boolean.TRUE.equals(semester.getIsCurrent())) {
            semesterRepository.clearCurrentExcept(semester.getId());
        }
    }

    private SchoolYear findSchoolYear(Long id) {
        return schoolYearRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.SCHOOL_YEAR_NOT_FOUND));
    }

    private SchoolYearItemDto toDto(SchoolYear schoolYear) {
        return SchoolYearItemDto.builder()
                .id(schoolYear.getId())
                .code(schoolYear.getCode())
                .name(schoolYear.getName())
                .startDate(schoolYear.getStartDate())
                .endDate(schoolYear.getEndDate())
                .status(schoolYear.getStatus())
                .isCurrent(schoolYear.getIsCurrent())
                .description(schoolYear.getDescription())
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new UserMessageException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateNoOverlappingSchoolYear(LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<SchoolYear> existingSchoolYears = schoolYearRepository.findAll();
        for (SchoolYear existing : existingSchoolYears) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            if (existing.getDeletedFlag() != null && existing.getDeletedFlag() == 1) {
                continue;
            }
            if (startDate != null && endDate != null &&
                    startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate())) {
                throw new UserMessageException(CommonErrorCode.SCHOOL_YEAR_DATE_OVERLAP);
            }
        }
    }

    private void validateSemesterUnique(Long schoolYearId, String code, String name, Integer semesterOrder, Long id) {
        semesterRepository.findBySchoolYearIdAndCode(schoolYearId, code)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_CODE_ALREADY_EXISTS);
                });
        semesterRepository.findBySchoolYearIdAndName(schoolYearId, name)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_NAME_ALREADY_EXISTS);
                });
        semesterRepository.findBySchoolYearIdAndSemesterOrder(schoolYearId, semesterOrder)
                .filter(item -> item.getDeletedFlag() == null || item.getDeletedFlag() == 0)
                .filter(item -> id == null || !item.getId().equals(id))
                .ifPresent(item -> {
                    throw new UserMessageException(CommonErrorCode.SEMESTER_ORDER_ALREADY_EXISTS);
                });
    }

    private void validateSemesterWithinSchoolYearDates(LocalDate semesterStart, LocalDate semesterEnd, SchoolYear schoolYear) {
        if (semesterStart != null && schoolYear.getStartDate() != null && semesterStart.isBefore(schoolYear.getStartDate())) {
            throw new UserMessageException(CommonErrorCode.SEMESTER_START_DATE_INVALID);
        }
        if (semesterEnd != null && schoolYear.getEndDate() != null && semesterEnd.isAfter(schoolYear.getEndDate())) {
            throw new UserMessageException(CommonErrorCode.SEMESTER_END_DATE_INVALID);
        }
    }

    private void validateNoOverlappingSemester(Long schoolYearId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<Semester> existingSemesters = semesterRepository.findBySchoolYearId(schoolYearId);
        for (Semester existing : existingSemesters) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            if (existing.getDeletedFlag() != null && existing.getDeletedFlag() == 1) {
                continue;
            }
            if (startDate != null && endDate != null &&
                    startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate())) {
                throw new UserMessageException(CommonErrorCode.SEMESTER_DATE_OVERLAP);
            }
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String statusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "Hoạt động" : "Không hoạt động";
    }

    private String booleanLabel(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Có" : "Không";
    }

    private String buildExportInfoLine() {
        String exportTime = LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        String username = SecurityUtils.getCurrentUsername();
        return "Thời gian tải: " + exportTime + " | Người tải: " + username;
    }

    private void setSchoolYearSheetWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 14 * 256);
        sheet.setColumnWidth(7, 32 * 256);
    }

    private void setSemesterSheetWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 24 * 256);
        sheet.setColumnWidth(4, 14 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 16 * 256);
        sheet.setColumnWidth(7, 16 * 256);
        sheet.setColumnWidth(8, 14 * 256);
        sheet.setColumnWidth(9, 32 * 256);
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
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createHighlightedImportCellStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        if (baseStyle != null) {
            style.cloneStyleFrom(baseStyle);
        }
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        // remove background fill for highlighted import cells (show error as red text only)
        style.setFillPattern(FillPatternType.NO_FILL);
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

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUsername();
    }

    @FunctionalInterface
    private interface SheetStartRowResolver {
        int resolve(Sheet sheet, DataFormatter formatter);
    }
}
