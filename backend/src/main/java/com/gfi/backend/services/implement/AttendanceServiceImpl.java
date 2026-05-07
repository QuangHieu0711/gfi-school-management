package com.gfi.backend.services.implement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.dtos.attendance.AttendanceBulkItemRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceBulkStudentRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceBulkUpsertRequest;
import com.gfi.backend.models.dtos.attendance.AttendanceImportResultDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableDto;
import com.gfi.backend.models.dtos.attendance.AttendanceMonthlyTableStudentDto;
import com.gfi.backend.models.dtos.common.TemporaryFileDto;
import com.gfi.backend.models.entities.AttendanceRecord;
import com.gfi.backend.models.entities.Classroom;
import com.gfi.backend.models.entities.Student;
import com.gfi.backend.models.entities.StudentEnrollment;
import com.gfi.backend.models.enums.ExportType;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.AttendanceRecordRepository;
import com.gfi.backend.repositories.ClassroomRepository;
import com.gfi.backend.repositories.StudentEnrollmentRepository;
import com.gfi.backend.repositories.StudentRepository;
import com.gfi.backend.services.interfaces.AttendanceService;
import com.gfi.backend.services.interfaces.ImportErrorFileStorageService;
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
public class AttendanceServiceImpl implements AttendanceService {

    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Set<String> VALID_SESSION_TYPES = Set.of("SANG", "CHIEU");
    private static final Set<String> VALID_STATUSES = Set.of("C", "P", "K", "X");

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ImportErrorFileStorageService importErrorFileStorageService;

    @Override
    @Transactional(readOnly = true)
    public AttendanceMonthlyTableDto getMonthlyTable(Long classroomId, Integer year, Integer month,
            String sessionType) {
        Classroom classroom = findClassroom(classroomId);
        String normalizedSessionType = normalizeSessionType(sessionType);
        YearMonth yearMonth = parseMonth(year, month);
        List<StudentEnrollment> enrollments = getActiveEnrollments(classroomId);
        List<Student> students = enrollments.stream().map(StudentEnrollment::getStudent).toList();

        LocalDate fromDate = yearMonth.atDay(1);
        LocalDate toDate = yearMonth.atEndOfMonth();
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByClassroomIdAndAttendanceDateBetweenAndSessionTypeAndDeletedFlagOrderByAttendanceDateAscStudentIdAsc(
                        classroomId, fromDate, toDate, normalizedSessionType, 0);

        Map<Long, Map<String, AttendanceRecord>> recordsByStudent = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            recordsByStudent
                    .computeIfAbsent(record.getStudent().getId(), ignored -> new LinkedHashMap<>())
                    .put(record.getAttendanceDate().toString(), record);
        }

        List<AttendanceMonthlyTableStudentDto> studentRows = new ArrayList<>();
        for (Student student : students) {
            Map<String, String> attendance = new LinkedHashMap<>();
            Map<String, AttendanceRecord> studentRecords = recordsByStudent.getOrDefault(student.getId(), Map.of());
            for (AttendanceRecord record : studentRecords.values()) {
                attendance.put(record.getAttendanceDate().toString(), record.getAttendanceStatus());
            }

            studentRows.add(AttendanceMonthlyTableStudentDto.builder()
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getFullName())
                    .attendance(attendance)
                    .build());
        }

        return AttendanceMonthlyTableDto.builder()
                .classroomId(classroom.getId())
                .classroomName(classroom.getName())
                .sessionType(normalizedSessionType)
                .year(yearMonth.getYear())
                .month(yearMonth.getMonthValue())
                .students(studentRows)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] export(Long classroomId, Integer year, Integer month, String sessionType, ExportType exportType) {
        return switch (exportType) {
            case EXCEL -> exportExcel(classroomId, year, month, sessionType);
            case PDF -> exportPdf(classroomId, year, month, sessionType);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcelTemplate(Long classroomId, Integer year, Integer month, String sessionType) {
        AttendanceMonthlyTableDto table = getMonthlyTable(classroomId, year, month, sessionType);
        Classroom classroom = findClassroom(classroomId);
        List<LocalDate> dates = buildMonthDates(YearMonth.of(table.getYear(), table.getMonth()));

        String titleText = buildExportTitle(table);
        String noteText = buildExportNote();

        int dateStartCol = 3;
        int kCol = dateStartCol + dates.size();
        int pCol = kCol + 1;
        int xCol = pCol + 1;
        int lastColumn = xCol;
        int headerRowIndex = 6;
        int dataStartRowIndex = headerRowIndex + 2;

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DiemDanh");

            CellStyle schoolStyle = createSchoolStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalInfoStyle = createTotalInfoStyle(workbook);
            CellStyle noteStyle = createNoteStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle totalGroupStyle = createTotalGroupStyle(workbook);
            CellStyle dayCellStyle = createDayCellStyle(workbook);
            CellStyle bodyCenterStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER, IndexedColors.WHITE);
            CellStyle bodyLeftStyle = createBodyStyle(workbook, HorizontalAlignment.LEFT, IndexedColors.WHITE);
            CellStyle saturdayBodyStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER, IndexedColors.LIGHT_YELLOW);
            CellStyle sundayBodyStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER, IndexedColors.GREY_25_PERCENT);

            Row topRow = sheet.createRow(0);
            createCell(topRow, 0, classroom.getUnit() == null ? "" : classroom.getUnit().getName(), schoolStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColumn));

            Row titleRow = sheet.createRow(1);
            createCell(titleRow, 0, titleText, titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastColumn));

            Row totalRow = sheet.createRow(2);
            Cell totalCell = createCell(totalRow, 0, null, totalInfoStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, lastColumn));

            Row noteRow = sheet.createRow(4);
            createCell(noteRow, 0, noteText, noteStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, lastColumn));

            Row dayNumberRow = sheet.createRow(headerRowIndex);
            Row dayNameRow = sheet.createRow(headerRowIndex + 1);

            createCell(dayNumberRow, 0, "STT", headerStyle);
            createCell(dayNumberRow, 1, "Mã HS", headerStyle);
            createCell(dayNumberRow, 2, "Họ và tên", headerStyle);
            createCell(dayNameRow, 0, "", headerStyle);
            createCell(dayNameRow, 1, "", headerStyle);
            createCell(dayNameRow, 2, "", headerStyle);

            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex + 1, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex + 1, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex + 1, 2, 2));

            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = dates.get(i);
                int col = dateStartCol + i;

                createCell(dayNumberRow, col, date.getDayOfMonth(), headerStyle);
                createCell(dayNameRow, col, shortDayOfWeek(date), dayCellStyle);

                tintHeaderForWeekend(dayNumberRow.getCell(col), workbook, date);
                tintHeaderForWeekend(dayNameRow.getCell(col), workbook, date);
            }

            createCell(dayNumberRow, kCol, "Tổng số", totalGroupStyle);
            createCell(dayNumberRow, pCol, "", totalGroupStyle);
            createCell(dayNumberRow, xCol, "", totalGroupStyle);
            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, kCol, xCol));

            createCell(dayNameRow, kCol, "K", totalGroupStyle);
            createCell(dayNameRow, pCol, "P", totalGroupStyle);
            createCell(dayNameRow, xCol, "X", totalGroupStyle);

            int rowIndex = dataStartRowIndex;
            int stt = 1;
            for (AttendanceMonthlyTableStudentDto student : table.getStudents()) {
                Row row = sheet.createRow(rowIndex++);

                createCell(row, 0, stt++, bodyCenterStyle);
                createCell(row, 1, student.getStudentCode(), bodyCenterStyle);
                createCell(row, 2, student.getStudentName(), bodyLeftStyle);

                Map<String, String> attendance = student.getAttendance() == null ? Map.of() : student.getAttendance();

                for (int i = 0; i < dates.size(); i++) {
                    LocalDate date = dates.get(i);
                    String status = attendance.get(date.toString());

                    CellStyle style = switch (date.getDayOfWeek()) {
                        case SATURDAY -> saturdayBodyStyle;
                        case SUNDAY -> sundayBodyStyle;
                        default -> bodyCenterStyle;
                    };

                    Cell cell = createCell(row, dateStartCol + i, status, style);
                    applyStatusFont(workbook, cell, status);
                }

                row.createCell(kCol).setCellFormula(buildCountIfFormula(row.getRowNum(), dateStartCol, kCol - 1, "K"));
                row.getCell(kCol).setCellStyle(bodyCenterStyle);
                row.createCell(pCol).setCellFormula(buildCountIfFormula(row.getRowNum(), dateStartCol, kCol - 1, "P"));
                row.getCell(pCol).setCellStyle(bodyCenterStyle);
                row.createCell(xCol).setCellFormula(buildCountIfFormula(row.getRowNum(), dateStartCol, kCol - 1, "X"));
                row.getCell(xCol).setCellStyle(bodyCenterStyle);
            }

            totalCell.setCellFormula(buildTotalSummaryFormula(dataStartRowIndex, rowIndex - 1, 1, kCol, pCol, xCol));
            setColumnWidths(sheet, dates.size(), dateStartCol, kCol);
            sheet.createFreezePane(3, headerRowIndex + 2);
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel mẫu điểm danh");
        }
    }

    @Override
    @Transactional
    public AttendanceImportResultDto importExcel(Long classroomId, Integer year, Integer month, String sessionType,
            MultipartFile file) {
        validateExcelFile(file);
        findClassroom(classroomId);
        String normalizedSessionType = normalizeSessionType(sessionType);
        YearMonth yearMonth = parseMonth(year, month);
        List<LocalDate> dates = buildMonthDates(yearMonth);
        Map<Integer, LocalDate> dateColumnMap = new HashMap<>();
        int dateStartCol = 3;
        for (int i = 0; i < dates.size(); i++) {
            dateColumnMap.put(dateStartCol + i, dates.get(i));
        }

        int successCount = 0;
        Map<Integer, String> rowErrors = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 8; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String studentCode = readCellText(row.getCell(1), formatter);
                if (!StringUtils.hasText(studentCode)) {
                    continue;
                }

                try {
                    Student student = studentRepository.findByStudentCode(studentCode.trim())
                            .orElseThrow(
                                    () -> new UserMessageException("Khong tim thay hoc sinh voi ma: " + studentCode));
                    validateStudentInClassroom(classroomId, student.getId());

                    for (Map.Entry<Integer, LocalDate> entry : dateColumnMap.entrySet()) {
                        String rawStatus = readCellText(row.getCell(entry.getKey()), formatter);
                        String normalizedStatus = normalizeStatus(rawStatus);
                        upsertAttendance(classroomId, student.getId(), entry.getValue(), normalizedSessionType,
                                normalizedStatus, null);
                        successCount++;
                    }
                } catch (Exception ex) {
                    rowErrors.put(rowIndex, ex.getMessage());
                }
            }

            String token = null;
            String errorFileName = null;
            if (!rowErrors.isEmpty()) {
                errorFileName = "attendance-import-errors.xlsx";
                token = importErrorFileStorageService.store(errorFileName, EXCEL_CONTENT_TYPE,
                        buildAttendanceImportErrorFile(workbook, sheet, rowErrors));
            }

            return AttendanceImportResultDto.builder()
                    .successCount(successCount)
                    .failedCount(rowErrors.size())
                    .hasErrorFile(token != null)
                    .errorFileToken(token)
                    .errorFileName(errorFileName)
                    .build();
        } catch (IOException ex) {
            throw new UserMessageException("Không đọc được file Excel");
        }
    }

    @Override
    public TemporaryFileDto getImportErrorFile(String token) {
        return importErrorFileStorageService.get(token);
    }

    @Override
    @Transactional
    public void bulkUpsert(AttendanceBulkUpsertRequest request) {
        String normalizedSessionType = normalizeSessionType(request.getSessionType());
        findClassroom(request.getClassroomId());
        for (AttendanceBulkItemRequest item : request.getItems()) {
            for (AttendanceBulkStudentRequest student : item.getStudents()) {
                upsertAttendance(
                        request.getClassroomId(),
                        student.getStudentId(),
                        item.getAttendanceDate(),
                        normalizedSessionType,
                        student.getStatus(),
                        student.getNote());
            }
        }
    }

    private byte[] exportExcel(Long classroomId, Integer year, Integer month, String sessionType) {
        AttendanceMonthlyTableDto table = getMonthlyTable(classroomId, year, month, sessionType);
        Classroom classroom = findClassroom(classroomId);
        List<LocalDate> dates = buildMonthDates(YearMonth.of(table.getYear(), table.getMonth()));
        SummaryCounter summary = summarize(table);

        String exportInfo = "Thời gian tải: " + LocalDateTime.now().format(EXPORT_TIME_FORMAT)
                + " | Người tải: " + SecurityUtils.getCurrentUsername();
        String titleText = buildExportTitle(table);
        String totalText = buildExportTotalLine(summary);
        String noteText = buildExportNote();

        int dateStartCol = 3;
        int kCol = dateStartCol + dates.size();
        int pCol = kCol + 1;
        int xCol = pCol + 1;
        int lastColumn = xCol;

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DiemDanh");

            CellStyle schoolStyle = createSchoolStyle(workbook);
            CellStyle exportInfoStyle = createExportInfoStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalInfoStyle = createTotalInfoStyle(workbook);
            CellStyle noteStyle = createNoteStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle totalGroupStyle = createTotalGroupStyle(workbook);
            CellStyle dayCellStyle = createDayCellStyle(workbook);
            CellStyle bodyCenterStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER, IndexedColors.WHITE);
            CellStyle bodyLeftStyle = createBodyStyle(workbook, HorizontalAlignment.LEFT, IndexedColors.WHITE);
            CellStyle saturdayBodyStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER,
                    IndexedColors.LIGHT_YELLOW);
            CellStyle sundayBodyStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER,
                    IndexedColors.GREY_25_PERCENT);

            Row topRow = sheet.createRow(0);
            createCell(topRow, 0, classroom.getUnit() == null ? "" : classroom.getUnit().getName(), schoolStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            createCell(topRow, 7, exportInfo, exportInfoStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 7, lastColumn));

            Row titleRow = sheet.createRow(1);
            createCell(titleRow, 0, titleText, titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastColumn));

            Row totalRow = sheet.createRow(2);
            createCell(totalRow, 0, totalText, totalInfoStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, lastColumn));

            Row noteRow = sheet.createRow(4);
            createCell(noteRow, 0, noteText, noteStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, lastColumn));

            int headerRowIndex = 6;
            Row dayNumberRow = sheet.createRow(headerRowIndex);
            Row dayNameRow = sheet.createRow(headerRowIndex + 1);

            createCell(dayNumberRow, 0, "STT", headerStyle);
            createCell(dayNumberRow, 1, "Mã HS", headerStyle);
            createCell(dayNumberRow, 2, "Họ và tên", headerStyle);

            createCell(dayNameRow, 0, "", headerStyle);
            createCell(dayNameRow, 1, "", headerStyle);
            createCell(dayNameRow, 2, "", headerStyle);

            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex + 1, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex + 1, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex + 1, 2, 2));

            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = dates.get(i);
                int col = dateStartCol + i;

                createCell(dayNumberRow, col, date.getDayOfMonth(), headerStyle);
                createCell(dayNameRow, col, shortDayOfWeek(date), dayCellStyle);

                tintHeaderForWeekend(dayNumberRow.getCell(col), workbook, date);
                tintHeaderForWeekend(dayNameRow.getCell(col), workbook, date);
            }

            createCell(dayNumberRow, kCol, "Tổng số", totalGroupStyle);
            createCell(dayNumberRow, pCol, "", totalGroupStyle);
            createCell(dayNumberRow, xCol, "", totalGroupStyle);
            sheet.addMergedRegion(new CellRangeAddress(headerRowIndex, headerRowIndex, kCol, xCol));

            createCell(dayNameRow, kCol, "K", totalGroupStyle);
            createCell(dayNameRow, pCol, "P", totalGroupStyle);
            createCell(dayNameRow, xCol, "X", totalGroupStyle);

            int rowIndex = headerRowIndex + 2;
            int stt = 1;

            for (AttendanceMonthlyTableStudentDto student : table.getStudents()) {
                Row row = sheet.createRow(rowIndex++);

                createCell(row, 0, stt++, bodyCenterStyle);
                createCell(row, 1, student.getStudentCode(), bodyCenterStyle);
                createCell(row, 2, student.getStudentName(), bodyLeftStyle);

                Map<String, String> attendance = student.getAttendance() == null ? Map.of() : student.getAttendance();

                for (int i = 0; i < dates.size(); i++) {
                    LocalDate date = dates.get(i);
                    String status = attendance.get(date.toString());

                    CellStyle style = switch (date.getDayOfWeek()) {
                        case SATURDAY -> saturdayBodyStyle;
                        case SUNDAY -> sundayBodyStyle;
                        default -> bodyCenterStyle;
                    };

                    Cell cell = createCell(row, dateStartCol + i, status, style);
                    applyStatusFont(workbook, cell, status);
                }

                createCell(row, kCol, countStatus(attendance, "K"), bodyCenterStyle);
                createCell(row, pCol, countStatus(attendance, "P"), bodyCenterStyle);
                createCell(row, xCol, countStatus(attendance, "X"), bodyCenterStyle);
            }

            setColumnWidths(sheet, dates.size(), dateStartCol, kCol);
            sheet.createFreezePane(3, headerRowIndex + 2);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel điểm danh");
        }
    }

    private byte[] exportPdf(Long classroomId, Integer year, Integer month, String sessionType) {
        AttendanceMonthlyTableDto table = getMonthlyTable(classroomId, year, month, sessionType);
        Classroom classroom = findClassroom(classroomId);
        List<LocalDate> dates = buildMonthDates(YearMonth.of(table.getYear(), table.getMonth()));
        SummaryCounter summary = summarize(table);

        String exportInfo = "Thời gian tải: " + LocalDateTime.now().format(EXPORT_TIME_FORMAT)
                + " | Người tải: " + SecurityUtils.getCurrentUsername();
        String titleText = buildExportTitle(table);
        String totalText = buildExportTotalLine(summary);
        String noteText = buildExportNote();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A3.rotate(), 16, 16, 16, 16);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font schoolFont = createPdfFont(11, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font titleFont = createPdfFont(15, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font infoFont = createPdfFont(9, com.lowagie.text.Font.ITALIC);
            com.lowagie.text.Font headerFont = createPdfFont(7, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(7, com.lowagie.text.Font.NORMAL);

            Paragraph school = new Paragraph(classroom.getUnit() == null ? "" : classroom.getUnit().getName(),
                    schoolFont);
            school.setAlignment(Element.ALIGN_LEFT);
            document.add(school);

            Paragraph exportMeta = new Paragraph(exportInfo, infoFont);
            exportMeta.setAlignment(Element.ALIGN_RIGHT);
            document.add(exportMeta);

            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(4f);
            document.add(title);

            Paragraph total = new Paragraph(totalText, infoFont);
            total.setAlignment(Element.ALIGN_CENTER);
            document.add(total);

            Paragraph note = new Paragraph(noteText, infoFont);
            note.setSpacingAfter(8f);
            document.add(note);

            PdfPTable pdfTable = new PdfPTable(buildPdfWidths(dates.size()));
            pdfTable.setWidthPercentage(100);
            pdfTable.setHeaderRows(2);
            pdfTable.setSplitLate(false);

            java.awt.Color totalGroupColor = new java.awt.Color(255, 228, 225);

            addPdfHeaderCell(pdfTable, "STT", headerFont, 2, 1, resolvePdfHeaderColor(null));
            addPdfHeaderCell(pdfTable, "Mã HS", headerFont, 2, 1, resolvePdfHeaderColor(null));
            addPdfHeaderCell(pdfTable, "Họ và tên", headerFont, 2, 1, resolvePdfHeaderColor(null));

            for (LocalDate date : dates) {
                addPdfHeaderCell(
                        pdfTable,
                        date.getDayOfMonth() + "\n" + shortDayOfWeek(date),
                        headerFont,
                        2,
                        1,
                        resolvePdfHeaderColor(date));
            }

            addPdfHeaderCell(pdfTable, "Tổng số", headerFont, 1, 3, totalGroupColor);
            addPdfHeaderCell(pdfTable, "K", headerFont, 1, 1, totalGroupColor);
            addPdfHeaderCell(pdfTable, "P", headerFont, 1, 1, totalGroupColor);
            addPdfHeaderCell(pdfTable, "X", headerFont, 1, 1, totalGroupColor);

            int stt = 1;
            for (AttendanceMonthlyTableStudentDto student : table.getStudents()) {
                Map<String, String> attendance = student.getAttendance() == null ? Map.of() : student.getAttendance();

                addPdfBodyCell(pdfTable, String.valueOf(stt++), bodyFont, Element.ALIGN_CENTER, null);
                addPdfBodyCell(pdfTable, student.getStudentCode(), bodyFont, Element.ALIGN_CENTER, null);
                addPdfBodyCell(pdfTable, student.getStudentName(), bodyFont, Element.ALIGN_LEFT, null);

                for (LocalDate date : dates) {
                    addPdfStatusCell(pdfTable, attendance.get(date.toString()), bodyFont, date);
                }

                addPdfBodyCell(pdfTable, String.valueOf(countStatus(attendance, "K")), bodyFont, Element.ALIGN_CENTER,
                        null);
                addPdfBodyCell(pdfTable, String.valueOf(countStatus(attendance, "P")), bodyFont, Element.ALIGN_CENTER,
                        null);
                addPdfBodyCell(pdfTable, String.valueOf(countStatus(attendance, "X")), bodyFont, Element.ALIGN_CENTER,
                        null);
            }

            document.add(pdfTable);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF điểm danh");
        }
    }

    private String buildExportTitle(AttendanceMonthlyTableDto table) {
        return "BẢNG ĐIỂM DANH THÁNG " + table.getMonth() + "/" + table.getYear()
                + " - " + table.getClassroomName().toUpperCase(Locale.ROOT)
                + " - " + getSessionTypeLabel(table.getSessionType());
    }

    private String buildExportTotalLine(SummaryCounter summary) {
        return "Tổng số học sinh: " + summary.totalStudents()
                + " | Tổng P: " + summary.totalP()
                + " | Tổng K: " + summary.totalK()
                + " | Tổng X: " + summary.totalX();
    }

    private String buildExportNote() {
        return "Ghi chú: C - Có mặt | P - Nghỉ có phép | K - Nghỉ không phép | X - Trường hợp khác (nghỉ học, đi muộn, về sớm...)";
    }

    private java.awt.Color resolvePdfHeaderColor(LocalDate date) {
        if (date == null) {
            return new java.awt.Color(183, 240, 240);
        }
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> new java.awt.Color(255, 249, 196);
            case SUNDAY -> new java.awt.Color(220, 220, 220);
            default -> new java.awt.Color(183, 240, 240);
        };
    }

    // private java.awt.Color resolvePdfBodyColor(LocalDate date) {
    // return switch (date.getDayOfWeek()) {
    // case SATURDAY -> new java.awt.Color(255, 249, 196);
    // case SUNDAY -> new java.awt.Color(220, 220, 220);
    // default -> null;
    // };
    // }

    private void addPdfHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font,
            int rowspan, int colspan, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        cell.setRowspan(rowspan);
        cell.setColspan(colspan);
        cell.setBackgroundColor(bgColor);
        table.addCell(cell);
    }

    private void addPdfBodyCell(PdfPTable table, String text, com.lowagie.text.Font font,
            int align, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        table.addCell(cell);
    }

    private void upsertAttendance(Long classroomId, Long studentId, LocalDate attendanceDate, String sessionType,
            String status, String note) {
        Classroom classroom = findClassroom(classroomId);
        Student student = findStudent(studentId);
        validateStudentInClassroom(classroom.getId(), student.getId());
        String normalizedStatus = normalizeStatus(status);

        AttendanceRecord record = attendanceRecordRepository
                .findByClassroomIdAndStudentIdAndAttendanceDateAndSessionType(
                        classroom.getId(),
                        student.getId(),
                        attendanceDate,
                        sessionType)
                .orElse(null);

        if (!StringUtils.hasText(normalizedStatus)) {
            if (record != null) {
                record.setDeletedFlag(1);
                record.setDeletedAt(LocalDateTime.now());
                record.setDeletedBy(SecurityUtils.getCurrentUsername());
                attendanceRecordRepository.save(record);
            }
            return;
        }

        if (record == null) {
            record = new AttendanceRecord();
            record.setClassroom(classroom);
            record.setStudent(student);
            record.setAttendanceDate(attendanceDate);
            record.setSessionType(sessionType);
            record.setCreatedBy(SecurityUtils.getCurrentUsername());
            record.setDeletedFlag(0);
        } else {
            record.setUpdatedBy(SecurityUtils.getCurrentUsername());
            record.setDeletedFlag(0);
            record.setDeletedAt(null);
            record.setDeletedBy(null);
        }

        record.setAttendanceStatus(normalizedStatus);
        record.setNote(normalizeNullable(note));
        attendanceRecordRepository.save(record);
    }

    private List<StudentEnrollment> getActiveEnrollments(Long classroomId) {
        return studentEnrollmentRepository.findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0);
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserMessageException("File import diem danh khong hop le");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new UserMessageException("File import phai la file Excel .xlsx");
        }
    }

    private String readCellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        String text = formatter.formatCellValue(cell);
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private byte[] buildAttendanceImportErrorFile(Workbook workbook, Sheet sheet, Map<Integer, String> rowErrors) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle errorStyle = createAttendanceImportErrorStyle(workbook);

        int headerRowIndex = 6;
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            headerRow = sheet.createRow(headerRowIndex);
        }

        int resultColumnIndex = headerRow.getLastCellNum() < 0 ? 40 : headerRow.getLastCellNum();
        int reasonColumnIndex = resultColumnIndex + 1;

        createCell(headerRow, resultColumnIndex, "Kết quả", headerStyle);
        createCell(headerRow, reasonColumnIndex, "Lý do lỗi", headerStyle);

        for (Map.Entry<Integer, String> entry : rowErrors.entrySet()) {
            Row row = sheet.getRow(entry.getKey());
            if (row == null) {
                row = sheet.createRow(entry.getKey());
            }
            createCell(row, resultColumnIndex, "Thất bại", errorStyle);
            for (int columnIndex = 0; columnIndex <= reasonColumnIndex; columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    cell = row.createCell(columnIndex);
                    cell.setCellValue("");
                }
                cell.setCellStyle(errorStyle);
            }
            createCell(row, reasonColumnIndex, entry.getValue(), errorStyle);
        }

        sheet.setColumnWidth(resultColumnIndex, 16 * 256);
        sheet.setColumnWidth(reasonColumnIndex, 42 * 256);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file lỗi import điểm danh");
        }
    }

    private CellStyle createAttendanceImportErrorStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook, HorizontalAlignment.LEFT, IndexedColors.WHITE);
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    private String getSessionTypeLabel(String sessionType) {
        if (sessionType == null) {
            return "";
        }

        return switch (sessionType.toUpperCase(Locale.ROOT)) {
            case "SANG" -> "SÁNG";
            case "CHIEU" -> "CHIỀU";
            default -> sessionType;
        };
    }

    private String buildCountIfFormula(int rowNumZeroBased, int fromColZeroBased, int toColZeroBased, String status) {
        int excelRow = rowNumZeroBased + 1;
        return "COUNTIF(" + toExcelColumnName(fromColZeroBased) + excelRow + ":"
                + toExcelColumnName(toColZeroBased) + excelRow + ",\"" + status + "\")";
    }

    private String buildTotalSummaryFormula(int startRowZeroBased, int endRowZeroBased, int studentCodeColZeroBased,
            int kColZeroBased, int pColZeroBased, int xColZeroBased) {
        int startRow = startRowZeroBased + 1;
        int endRow = endRowZeroBased + 1;
        return "\"Tổng số học sinh: \"&COUNTA(" + toExcelColumnName(studentCodeColZeroBased) + startRow + ":"
                + toExcelColumnName(studentCodeColZeroBased) + endRow + ")"
                + "&\" | Tổng P: \"&SUM(" + toExcelColumnName(pColZeroBased) + startRow + ":"
                + toExcelColumnName(pColZeroBased) + endRow + ")"
                + "&\" | Tổng K: \"&SUM(" + toExcelColumnName(kColZeroBased) + startRow + ":"
                + toExcelColumnName(kColZeroBased) + endRow + ")"
                + "&\" | Tổng X: \"&SUM(" + toExcelColumnName(xColZeroBased) + startRow + ":"
                + toExcelColumnName(xColZeroBased) + endRow + ")";
    }

    private String toExcelColumnName(int columnIndexZeroBased) {
        int dividend = columnIndexZeroBased + 1;
        StringBuilder columnName = new StringBuilder();
        while (dividend > 0) {
            int modulo = (dividend - 1) % 26;
            columnName.insert(0, (char) ('A' + modulo));
            dividend = (dividend - modulo - 1) / 26;
        }
        return columnName.toString();
    }

    private List<LocalDate> buildMonthDates(YearMonth yearMonth) {
        List<LocalDate> dates = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            dates.add(yearMonth.atDay(day));
        }
        return dates;
    }

    private SummaryCounter summarize(AttendanceMonthlyTableDto table) {
        int totalP = 0;
        int totalK = 0;
        int totalX = 0;
        for (AttendanceMonthlyTableStudentDto student : table.getStudents()) {
            Map<String, String> attendance = student.getAttendance() == null ? Map.of() : student.getAttendance();
            totalP += countStatus(attendance, "P");
            totalK += countStatus(attendance, "K");
            totalX += countStatus(attendance, "X");
        }
        return new SummaryCounter(table.getStudents().size(), totalP, totalK, totalX);
    }

    private int countStatus(Map<String, String> attendance, String status) {
        return (int) attendance.values().stream().filter(status::equals).count();
    }

    private String shortDayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private void setColumnWidths(Sheet sheet, int dateCount, int dateStartCol, int totalGroupCol) {
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        for (int i = 0; i < dateCount; i++) {
            sheet.setColumnWidth(dateStartCol + i, 5 * 256);
        }
        sheet.setColumnWidth(totalGroupCol, 6 * 256);
        sheet.setColumnWidth(totalGroupCol + 1, 6 * 256);
        sheet.setColumnWidth(totalGroupCol + 2, 6 * 256);
    }

    private CellStyle createSchoolStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createExportInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 9);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 17);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createTotalInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createNoteStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(style);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createTotalGroupStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(createHeaderStyle(workbook));
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDayCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(createHeaderStyle(workbook));
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        font.setFontName(EXPORT_FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook, HorizontalAlignment alignment, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorder(style);
        Font font = workbook.createFont();
        font.setFontName(EXPORT_FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private Cell createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value != null) {
            cell.setCellValue(String.valueOf(value));
        } else {
            cell.setCellValue("");
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
        return cell;
    }

    private void tintHeaderForWeekend(Cell cell, Workbook workbook, LocalDate date) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(cell.getCellStyle());
        if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        }
        if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);
    }

    private void applyStatusFont(Workbook workbook, Cell cell, String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(cell.getCellStyle());
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName(EXPORT_FONT_NAME);
        font.setColor(switch (status) {
            case "P" -> IndexedColors.GREEN.getIndex();
            case "K" -> IndexedColors.RED.getIndex();
            case "X" -> IndexedColors.ORANGE.getIndex();
            case "C" -> IndexedColors.BLUE.getIndex();
            default -> IndexedColors.BLACK.getIndex();
        });
        style.setFont(font);
        cell.setCellStyle(style);
    }

    @SuppressWarnings("unused")
    private void addPdfHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new java.awt.Color(183, 240, 240));
        cell.setPadding(3f);
        table.addCell(cell);
    }

    @SuppressWarnings("unused")
    private void addPdfBodyCell(PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private void addPdfStatusCell(PdfPTable table, String status, com.lowagie.text.Font bodyFont, LocalDate date) {
        com.lowagie.text.Font font = bodyFont;
        if (status != null) {
            font = switch (status) {
                case "P" -> createPdfColoredFont(7, com.lowagie.text.Font.BOLD, java.awt.Color.GREEN.darker());
                case "K" -> createPdfColoredFont(7, com.lowagie.text.Font.BOLD, java.awt.Color.RED);
                case "X" -> createPdfColoredFont(7, com.lowagie.text.Font.BOLD, java.awt.Color.ORANGE.darker());
                case "C" -> createPdfColoredFont(7, com.lowagie.text.Font.BOLD, java.awt.Color.BLUE);
                default -> bodyFont;
            };
        }
        PdfPCell cell = new PdfPCell(new Phrase(status == null ? "" : status, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
            cell.setBackgroundColor(new java.awt.Color(255, 249, 196));
        }
        if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
        }
        table.addCell(cell);
    }

    private float[] buildPdfWidths(int dateCount) {
        float[] widths = new float[dateCount + 6];
        widths[0] = 0.8f;
        widths[1] = 1.6f;
        widths[2] = 3.0f;
        for (int i = 0; i < dateCount; i++) {
            widths[3 + i] = 0.55f;
        }
        widths[3 + dateCount] = 0.6f;
        widths[4 + dateCount] = 0.6f;
        widths[5 + dateCount] = 0.6f;
        return widths;
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

    private com.lowagie.text.Font createPdfColoredFont(float size, int style, java.awt.Color color) {
        com.lowagie.text.Font font = createPdfFont(size, style);
        font.setColor(color);
        return font;
    }

    private Classroom findClassroom(Long id) {
        return classroomRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.CLASS_NOT_FOUND));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.STUDENT_NOT_FOUND));
    }

    private void validateStudentInClassroom(Long classroomId, Long studentId) {
        boolean exists = studentEnrollmentRepository
                .findByClassroomIdAndDeletedFlagOrderByStudentFullNameAsc(classroomId, 0)
                .stream()
                .anyMatch(enrollment -> enrollment.getStudent().getId().equals(studentId));
        if (!exists) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(),
                    "Học sinh không thuộc lớp học");
        }
    }

    private YearMonth parseMonth(Integer year, Integer month) {
        try {
            return YearMonth.of(year, month);
        } catch (Exception ex) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Năm hoặc tháng không hợp lệ");
        }
    }

    private String normalizeSessionType(String sessionType) {
        String normalized = normalize(sessionType);
        if (!StringUtils.hasText(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(), "Buổi điểm danh là bắt buộc");
        }
        if (!VALID_SESSION_TYPES.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(),
                    "Buổi điểm danh chỉ hỗ trợ SÁNG hoặc CHIỀU");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!VALID_STATUSES.contains(normalized)) {
            throw new UserMessageException(CommonErrorCode.INVALID_REQUEST.getCode(),
                    "Trạng thái điểm danh chỉ hỗ trợ C, P, K, X");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record SummaryCounter(int totalStudents, int totalP, int totalK, int totalX) {
    }
}
