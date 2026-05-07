package com.gfi.backend.services.implement;

import java.time.LocalDateTime;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.gfi.backend.controllers.exceptions.UserMessageException;
import com.gfi.backend.models.enums.ExportType;
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
import java.nio.file.Files;
import java.nio.file.Path;
import com.gfi.backend.models.dtos.common.LookupItemDto;
import com.gfi.backend.models.dtos.menu.MenuCreateRequest;
import com.gfi.backend.models.dtos.menu.MenuDetailDto;
import com.gfi.backend.models.dtos.menu.MenuFilterDto;
import com.gfi.backend.models.dtos.menu.MenuListItemDto;
import com.gfi.backend.models.dtos.menu.MenuUpdateRequest;
import com.gfi.backend.models.entities.Menu;
import com.gfi.backend.models.global.CommonErrorCode;
import com.gfi.backend.repositories.DataPermissionRepository;
import com.gfi.backend.repositories.MenuRepository;
import com.gfi.backend.repositories.PermissionRepository;
import com.gfi.backend.repositories.specifications.MenuSpecification;
import com.gfi.backend.services.interfaces.MenuService;
import com.gfi.backend.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Triển khai service cho quản lý menu.
 * Xử lý các thao tác CRUD và nghiệp vụ cho thực thể menu.
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final PermissionRepository permissionRepository;
    private final DataPermissionRepository dataPermissionRepository;
    private final MenuSpecification menuSpecification;

    @Override
    public List<MenuListItemDto> search(MenuFilterDto filter) {
        MenuFilterDto safeFilter = filter == null ? new MenuFilterDto() : filter;
        return menuRepository.findAll(menuSpecification.buildSpecification(safeFilter), Sort.by(Sort.Direction.ASC, "ordinal", "name"))
                .stream()
                .map(this::toListDto)
                .toList();
    }

    @Override
    public List<LookupItemDto> getOptions() {
        return menuRepository.findAll(menuSpecification.buildActiveOnlySpecification(), Sort.by(Sort.Direction.ASC, "ordinal", "name"))
                .stream()
                .map(menu -> LookupItemDto.builder()
                        .id(menu.getId())
                        .name(menu.getName())
                        .build())
                .toList();
    }

    @Override
    public MenuDetailDto getById(Long id) {
        return toDetailDto(findMenu(id));
    }

    @Override
    @Transactional
    public MenuDetailDto create(MenuCreateRequest request) {
        // Normalize and validate code uniqueness
        String code = normalize(request.getCode());
        validateUniqueCode(code, null);

        // Create new menu entity and map request data
        Menu menu = new Menu();
        mapCreateRequestToEntity(request, menu);
        menu.setCreatedBy(SecurityUtils.getCurrentUsername());

        return toDetailDto(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public MenuDetailDto update(Long id, MenuUpdateRequest request) {
        Menu menu = findMenu(id);

        // Kiểm tra mã mới có trùng với menu khác không
        String code = normalize(request.getCode());
        validateUniqueCode(code, id);

        // Cập nhật thông tin menu từ request
        mapUpdateRequestToEntity(request, menu);
        menu.setUpdatedBy(SecurityUtils.getCurrentUsername());

        return toDetailDto(menuRepository.save(menu));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Menu menu = findMenu(id);

        // Kiểm tra nếu menu đang được sử dụng trong permissions
        if (permissionRepository.countByMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_IN_USE);
        }
        if (dataPermissionRepository.countByMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_IN_USE);
        }

        // Kiểm tra nếu menu có menu con (chỉ lấy những menu chưa xóa)
        if (menuRepository.countActiveByParentMenuId(id) > 0) {
            throw new UserMessageException(CommonErrorCode.MENU_HAS_CHILDREN);
        }

        // Xóa mềm: đánh dấu xóa thay vì hard delete
        menu.setDeletedFlag(1);
        menu.setDeletedAt(LocalDateTime.now());
        menu.setDeletedBy(SecurityUtils.getCurrentUsername());
        menuRepository.save(menu);
    }

    /**
     * Ánh xa dữ liệu từ MenuCreateRequest sang Menu entity.
     * Xử lý validate parent menu và normalize các trường dữ liệu.
     */
    private void mapCreateRequestToEntity(MenuCreateRequest request, Menu menu) {
        validateParent(null, request.getParentId());

        if (request.getParentId() != null) {
            menu.setParentMenu(findMenu(request.getParentId()));
        }

        menu.setCode(normalize(request.getCode()));
        menu.setName(normalize(request.getName()));
        menu.setUrl(normalizeNullable(request.getUrl()));
        menu.setIcon(normalizeNullable(request.getIcon()));
        menu.setOrdinal(request.getOrdinal() == null ? 0 : request.getOrdinal());
    }

    /**
     * Ánh xa dữ liệu từ MenuUpdateRequest sang Menu entity.
     * Xử lý validate parent menu và normalize các trường dữ liệu.
     */
    private void mapUpdateRequestToEntity(MenuUpdateRequest request, Menu menu) {
        validateParent(menu.getId(), request.getParentId());

        if (request.getParentId() != null) {
            menu.setParentMenu(findMenu(request.getParentId()));
        } else {
            menu.setParentMenu(null);
        }

        menu.setCode(normalize(request.getCode()));
        menu.setName(normalize(request.getName()));
        menu.setUrl(normalizeNullable(request.getUrl()));
        menu.setIcon(normalizeNullable(request.getIcon()));
        menu.setOrdinal(request.getOrdinal() == null ? 0 : request.getOrdinal());
    }

    /**
     * Kiểm tra tính duy nhất của mã menu.
     * Chỉ kiểm tra các menu chưa xóa (deletedFlag = 0).
     *
     * @param code      Mã cần kiểm tra
     * @param excludeId ID menu cần loại trừ khỏi kiểm tra
     */
    private void validateUniqueCode(String code, Long excludeId) {
        menuRepository.findByCodeAndDeletedFlagZero(code)
                .filter(found -> excludeId == null || !found.getId().equals(excludeId))
                .ifPresent(found -> {
                    throw new UserMessageException(CommonErrorCode.MENU_CODE_ALREADY_EXISTS);
                });
    }

    /**
     * Kiểm tra tính hợp lệ của menu cha.
     *
     * @param menuId   ID của menu hiện tại
     * @param parentId ID của menu cha được đề xuất
     */
    private void validateParent(Long menuId, Long parentId) {
        if (menuId != null && parentId != null && menuId.equals(parentId)) {
            throw new UserMessageException(CommonErrorCode.BAD_REQUEST);
        }
    }

    /**
     * Tìm menu theo ID, chỉ lấy menu chưa xóa (deletedFlag = 0).
     * Nếu không tìm thấy sẽ ném UserMessageException với lỗi MENU_NOT_FOUND.
     */
    private Menu findMenu(Long id) {
        return menuRepository.findById(id)
                .filter(menu -> menu.getDeletedFlag() == 0)
                .orElseThrow(() -> new UserMessageException(CommonErrorCode.MENU_NOT_FOUND));
    }

    /**
     * Chuyển đổi Menu entity sang MenuListItemDto (cho danh sách).
     * Chỉ chứa thông tin cơ bản cần thiết cho hiển thị trong danh sách
     */
    private MenuListItemDto toListDto(Menu menu) {
        return MenuListItemDto.builder()
                .id(menu.getId())
                .code(menu.getCode())
                .name(menu.getName())
                .parentCode(menu.getParentMenu() == null ? null : menu.getParentMenu().getCode())
                .ordinal(menu.getOrdinal())
                .icon(menu.getIcon())
                .url(menu.getUrl())
                .build();
    }

    /**
     * Chuyển đổi Menu entity sang MenuDetailDto (cho chi tiết).
     * Chỉ chứa thông tin chi tiết cần thiết cho hiển thị trong phần chi tiết menu
     */
    private MenuDetailDto toDetailDto(Menu menu) {
        return MenuDetailDto.builder()
                .id(menu.getId())
                .parentId(menu.getParentMenu() == null ? null : menu.getParentMenu().getId())
                .parentCode(menu.getParentMenu() == null ? null : menu.getParentMenu().getCode())
                .code(menu.getCode())
                .name(menu.getName())
                .url(menu.getUrl())
                .icon(menu.getIcon())
                .ordinal(menu.getOrdinal())
                .build();
    }

    /**
     * Kiểm tra xem chuỗi có chứa nội dung hay không
     * (khác null và không rỗng sau khi loại bỏ khoảng trắng).
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Chuẩn hóa chuỗi bằng cách loại bỏ khoảng trắng ở đầu và cuối.
     * Trả về null nếu giá trị đầu vào là null.
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Chuẩn hóa chuỗi có thể null bằng cách loại bỏ khoảng trắng ở đầu và cuối.
     * Trả về null nếu giá trị đầu vào null hoặc rỗng sau khi trim.
     */
    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }

    @Override
    public byte[] export(MenuFilterDto filter, ExportType exportType) {
        List<MenuListItemDto> items = search(filter);
        if (exportType == ExportType.PDF) {
            return exportMenusPdf(items);
        }
        return exportMenusExcel(items);
    }

    private byte[] exportMenusExcel(List<MenuListItemDto> items) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Menus");
            int rowIndex = 0;
            Row header = sheet.createRow(rowIndex++);
            String[] headers = new String[] { "Menu ID", "Menu Code", "Menu Name", "Parent Code", "URL", "Icon", "Ordinal" };
            CellStyle headerStyle = createExportHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setAlignment(HorizontalAlignment.LEFT);
            Font bodyFont = workbook.createFont();
            bodyFont.setFontName("Times New Roman");
            bodyStyle.setFont(bodyFont);

            for (MenuListItemDto item : items) {
                Row row = sheet.createRow(rowIndex++);
                int c = 0;
                Cell cell = row.createCell(c++);
                cell.setCellValue(item.getId() == null ? "" : String.valueOf(item.getId()));
                cell = row.createCell(c++);
                cell.setCellValue(item.getCode());
                cell = row.createCell(c++);
                cell.setCellValue(item.getName());
                cell = row.createCell(c++);
                cell.setCellValue(item.getParentCode());
                cell = row.createCell(c++);
                cell.setCellValue(item.getUrl());
                cell = row.createCell(c++);
                cell.setCellValue(item.getIcon());
                cell = row.createCell(c++);
                cell.setCellValue(item.getOrdinal() == null ? 0 : item.getOrdinal());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new UserMessageException("Không thể tạo file Excel menu");
        }
    }

    private byte[] exportMenusPdf(List<MenuListItemDto> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 20, 20);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = createPdfFont(10, com.lowagie.text.Font.NORMAL);

            Paragraph title = new Paragraph("DANH SÁCH MENU", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[] { 1.2f, 2.0f, 3.0f, 2.0f, 3.0f, 2.0f, 1.0f });
            table.setWidthPercentage(100);

            addPdfHeaderCell(table, "Menu ID", headerFont);
            addPdfHeaderCell(table, "Menu Code", headerFont);
            addPdfHeaderCell(table, "Menu Name", headerFont);
            addPdfHeaderCell(table, "Parent Code", headerFont);
            addPdfHeaderCell(table, "URL", headerFont);
            addPdfHeaderCell(table, "Icon", headerFont);
            addPdfHeaderCell(table, "Ordinal", headerFont);

            for (MenuListItemDto item : items) {
                addPdfBodyCell(table, item.getId() == null ? "" : String.valueOf(item.getId()), bodyFont, Element.ALIGN_CENTER);
                addPdfBodyCell(table, item.getCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getName(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getParentCode(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getUrl(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getIcon(), bodyFont, Element.ALIGN_LEFT);
                addPdfBodyCell(table, item.getOrdinal() == null ? "0" : String.valueOf(item.getOrdinal()), bodyFont, Element.ALIGN_CENTER);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new UserMessageException("Không thể tạo file PDF menu");
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
            // Fallback to font factory below.
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

    private static final String EXPORT_FONT_NAME = "Times New Roman";
    private static final String TIMES_FONT_REGULAR_PATH = "C:/Windows/Fonts/times.ttf";
    private static final String TIMES_FONT_BOLD_PATH = "C:/Windows/Fonts/timesbd.ttf";
    private static final String TIMES_FONT_ITALIC_PATH = "C:/Windows/Fonts/timesi.ttf";
}
