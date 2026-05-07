/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { defaultExportFileName, saveBlobAsFile } from '@utils/file-util';

import {
  MENU_FILTER_FORM,
  MENU_KEY,
  MenuResponse,
  MenuTreeRow,
  MenuExportRequest,
} from '@app/model/admin/menu.model';
import { MenuService } from '@app/service/admin/menu.service';
import { DialogMenuComponent } from './dialog-menu/dialog-menu.component';
import { PermissionCheckService } from '@service';

@Component({
  selector: 'menu-management',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class MenuComponent extends ComponentBaseAbstract {
  @ViewChild('nameTpl', { static: true })
  nameTpl!: TemplateRef<unknown>;

  @ViewChild('parentTpl', { static: true })
  parentTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'FUNCTION_MANAGEMENT';
  tableConfig = {
    hasFilterPanel: true,
    showPaginator: false,
  };
  columns: MtxGridColumn[] = [];
  $formItem = MENU_FILTER_FORM;
  key = MENU_KEY;
  dataSource: MenuTreeRow[] = [];
  private allMenus: MenuResponse[] = [];
  private treeRows: MenuTreeRow[] = [];
  private readonly expandedIds = new Set<unknown>();

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canDownload(): boolean {
    return this.permissionCheckService.canDownload(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly menuService: MenuService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.menuService.getOptions().subscribe();

    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Cấu trúc chức năng',
        field: MENU_KEY.NAME,
        cellTemplate: this.nameTpl,
      },
      {
        header: 'Mã chức năng',
        field: MENU_KEY.CODE,
      },
      {
        header: 'Menu cha',
        field: MENU_KEY.PARENT_NAME,
        cellTemplate: this.parentTpl,
      },
      {
        header: 'Đường dẫn',
        field: MENU_KEY.URL,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            tooltip: 'Chỉnh sửa',
            click: (rowData: MenuTreeRow) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            tooltip: 'Xóa',
            click: (rowData: MenuTreeRow) => this.deleteMenu(rowData),
          },
        ],
      },
    ];

    this.loadMenus();
  }

  filterData() {
    this.pageIndex = 0;
    this.applyTreeFilter();
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  toggleRow(rowData: MenuTreeRow, event?: Event) {
    event?.stopPropagation();

    if (!rowData[MENU_KEY.CHILDREN_COUNT]) return;

    if (this.expandedIds.has(rowData.id)) {
      this.expandedIds.delete(rowData.id);
    } else {
      this.expandedIds.add(rowData.id);
    }

    this.dataSource = this.getVisibleRows();
  }

  hasChildren(rowData: MenuTreeRow): boolean {
    return Number(rowData[MENU_KEY.CHILDREN_COUNT] ?? 0) > 0;
  }

  isExpanded(rowData: MenuTreeRow): boolean {
    return this.expandedIds.has(rowData.id);
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: MenuTreeRow) {
    this.dialog.componentDialog(
      DialogMenuComponent,
      {
        width: '640px',
        data: {
          type,
          id: rowData?.[MENU_KEY.ID],
        },
      },
      (result?: boolean) => {
        if (result) {
          this.filterData();
        }
      }
    );
  }

  deleteMenu(rowData: MenuTreeRow) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa chức năng ${rowData[MENU_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.menuService.delete(rowData[MENU_KEY.ID]).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');

            this.filterData();
          },
          error: (error) => {
            this.toastr.error(
              error?.error?.userMessage ??
                error?.error?.message ??
                'Xóa thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  exportExcel(): void {
    if (!this.canDownload) {
      this.toastr.warning('Bạn không có quyền tải xuống', 'Cảnh báo');
      return;
    }
    this.exportFile('EXCEL');
  }

  exportPdf(): void {
    if (!this.canDownload) {
      this.toastr.warning('Bạn không có quyền tải xuống', 'Cảnh báo');
      return;
    }
    this.exportFile('PDF');
  }

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const formValues = this.form.getRawValue();
    const payload: MenuExportRequest = {
      exportType,
      filter: {
        menu: formValues['menu'],
      },
    };

    this.menuService.export(payload).subscribe({
      next: (res: any) => {
        this.toastr.removeToastr();

        const blob = this.extractBlob(res);
        if (!blob) {
          this.toastr.error(
            `Xuất ${exportType} thất bại: Dữ liệu không hợp lệ`,
            'Lỗi'
          );
          return;
        }

        const ext = exportType === 'PDF' ? 'pdf' : 'xlsx';
        const fallbackName = defaultExportFileName('menu', ext);
        const disposition = this.getHeader(res, 'content-disposition');
        const fileName = this.getFileNameFromDisposition(
          disposition,
          fallbackName
        );

        saveBlobAsFile(blob, fileName);
        this.toastr.success(
          `Tải xuống ${exportType} thành công`,
          `Xuất ${exportType}`
        );
      },
      error: () => {
        this.toastr.removeToastr();
        this.toastr.error(`Xuất ${exportType} thất bại`, 'Lỗi');
      },
    });
  }

  private extractBlob(res: any): Blob | null {
    if (res instanceof Blob) return res;
    if (res?.body instanceof Blob) return res.body;
    if (res?.data instanceof Blob) return res.data;
    return null;
  }

  private getHeader(res: any, headerName: string): string | null {
    if (res?.headers?.get) return res.headers.get(headerName);

    const headers = res?.headers;
    if (headers && typeof headers === 'object') {
      const key = headerName.toLowerCase();
      return headers[headerName] ?? headers[key] ?? null;
    }

    return null;
  }

  private getFileNameFromDisposition(
    disposition: string | null,
    fallbackName: string
  ): string {
    if (!disposition) return fallbackName;

    const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)/i);
    if (!match?.[1]) return fallbackName;

    const rawFileName = match[1].trim();

    try {
      return this.decodeMimeFileName(decodeURIComponent(rawFileName));
    } catch {
      return this.decodeMimeFileName(rawFileName);
    }
  }

  private decodeMimeFileName(fileName: string): string {
    const mimeMatch = fileName.match(/^=\?UTF-8\?Q\?(.+)\?=$/i);
    if (!mimeMatch?.[1]) return fileName;

    const normalized = mimeMatch[1].replace(/_/g, ' ');
    const decoded = normalized.replace(/=([0-9A-F]{2})/gi, '%$1');

    try {
      return decodeURIComponent(decoded);
    } catch {
      return fileName;
    }
  }

  private buildTreeRows(items: MenuResponse[]): MenuTreeRow[] {
    const itemMap = new Map(items.map((item) => [item.id, item]));
    const childrenMap = new Map<unknown, MenuResponse[]>();

    for (const item of items) {
      const key = item.parentId ?? null;
      const siblings = childrenMap.get(key) ?? [];
      siblings.push(item);
      childrenMap.set(key, siblings);
    }

    const roots = items.filter(
      (item) => item.parentId == null || !itemMap.has(item.parentId)
    );

    const flattened: MenuTreeRow[] = [];
    const visited = new Set<unknown>();

    const appendNode = (node: MenuResponse, level: number) => {
      if (visited.has(node.id)) return;
      visited.add(node.id);

      const children = [...(childrenMap.get(node.id) ?? [])].sort(
        (a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0)
      );
      const parent = node.parentId != null ? itemMap.get(node.parentId) : null;

      flattened.push({
        ...node,
        parentName: parent?.name ?? null,
        level,
        childrenCount: children.length,
      });

      children.forEach((child) => appendNode(child, level + 1));
    };

    [...roots]
      .sort((a, b) => (a.ordinal ?? 0) - (b.ordinal ?? 0))
      .forEach((root) => appendNode(root, 0));

    items
      .filter((item) => !visited.has(item.id))
      .forEach((item) => appendNode(item, 0));

    return flattened;
  }

  private syncExpandedState() {
    const validIds = new Set<unknown>(
      this.treeRows.filter((row) => this.hasChildren(row)).map((row) => row.id)
    );

    [...this.expandedIds].forEach((id) => {
      if (!validIds.has(id)) {
        this.expandedIds.delete(id);
      }
    });

    this.treeRows
      .filter((row) => row[MENU_KEY.LEVEL] === 0 && this.hasChildren(row))
      .forEach((row) => this.expandedIds.add(row.id));
  }

  private getVisibleRows(): MenuTreeRow[] {
    return this.treeRows.filter((row) => this.isRowVisible(row));
  }

  private isRowVisible(row: MenuTreeRow): boolean {
    if (row[MENU_KEY.LEVEL] === 0) return true;

    const parentId = row[MENU_KEY.PARENT_ID];
    if (parentId == null || !this.expandedIds.has(parentId)) {
      return false;
    }

    const parentRow = this.treeRows.find((item) => item.id === parentId);
    return parentRow ? this.isRowVisible(parentRow) : true;
  }

  private normalizeMenus(items: any[]): MenuResponse[] {
    const normalized = (items ?? []).map((raw) => ({
      ...raw,
      id: raw.menuId ?? raw.id,
      name: raw.menuName ?? raw.name,
      code: raw.menuCode ?? raw.code,
      url: raw.menuUrl ?? raw.url,
      parentCode: raw.parentCode ?? raw.menuParentCode ?? null,
    }));

    // Dedup theo id
    const uniqueMap = new Map<unknown, any>();
    normalized.forEach((item) => {
      if (!uniqueMap.has(item.id)) {
        uniqueMap.set(item.id, item);
      }
    });

    const deduped = Array.from(uniqueMap.values());

    // Map parentCode → parentId
    const codeToId = new Map(deduped.map((item) => [item.code, item.id]));
    return deduped.map((item) => ({
      ...item,
      parentId: item.parentCode
        ? (codeToId.get(item.parentCode) ?? null)
        : null,
    }));
  }

  private loadMenus() {
    this.menuService.filter({}).subscribe({
      next: ({ data }) => {
        this.allMenus = this.normalizeMenus(data ?? []);
        this.applyTreeFilter();
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách chức năng',
          'Thất bại'
        );
      },
    });
  }

  private applyTreeFilter() {
    const keyword = `${this.form.getRawValue().menu ?? ''}`
      .trim()
      .toLowerCase();
    const filteredItems = keyword
      ? this.filterMenusByKeyword(this.allMenus, keyword)
      : this.allMenus;

    this.treeRows = this.buildTreeRows(filteredItems);
    this.syncExpandedState();
    this.dataSource = this.getVisibleRows();
    this.dataSourceTotal = filteredItems.length;
    this.pageSize = this.dataSource.length || 1000;
  }

  private filterMenusByKeyword(
    items: MenuResponse[],
    keyword: string
  ): MenuResponse[] {
    const itemMap = new Map(items.map((item) => [item.id, item]));
    const matchedIds = new Set(
      items
        .filter((item) => {
          const code = `${item.code ?? ''}`.toLowerCase();
          const name = `${item.name ?? ''}`.toLowerCase();
          return code.includes(keyword) || name.includes(keyword);
        })
        .map((item) => item.id)
    );

    const resultIds = new Set<unknown>();

    matchedIds.forEach((id) => {
      let current = itemMap.get(id);
      while (current) {
        resultIds.add(current.id);
        current =
          current.parentId != null ? itemMap.get(current.parentId) : undefined;
      }
    });

    return items.filter((item) => resultIds.has(item.id));
  }
}
