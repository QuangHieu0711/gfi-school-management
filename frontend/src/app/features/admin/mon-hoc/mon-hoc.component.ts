/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { defaultExportFileName, saveBlobAsFile } from '@utils/file-util';

import {
  MON_HOC_FILTER_FORM,
  MON_HOC_KEY,
  MON_HOC_TYPE_OPTIONS,
  MonHocExportRequest,
  MonHocResponse,
} from '@app/model/admin/mon-hoc.model';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { DialogMonHocComponent } from './dialog-mon-hoc/dialog-mon-hoc.component';
import { PermissionCheckService } from '@service';
import { DialogImportMonHocComponent } from './dialog-import/dialog-import.component';

@Component({
  selector: 'mon-hoc',
  templateUrl: './mon-hoc.component.html',
  styleUrls: ['./mon-hoc.component.scss'],
  imports: [
    CommonModule,
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class MonHocComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'SUBJECT_MANAGEMENT';
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = MON_HOC_FILTER_FORM;
  key = MON_HOC_KEY;
  dataSource: MonHocResponse[] = [];
  showAdvancedFilters = false;

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canDownload(): boolean {
    return this.permissionCheckService.canDownload(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly monHocService: MonHocService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.columns = [
      { header: 'STT', class: 'text-center', field: COMMON_TABLE_KEY.STT },
      { header: 'Mã môn học', field: MON_HOC_KEY.CODE },
      { header: 'Tên môn học', field: MON_HOC_KEY.NAME },
      {
        header: 'Loại',
        field: MON_HOC_KEY.TYPE,
        formatter: (data: MonHocResponse) =>
          this.getTypeLabel(data[MON_HOC_KEY.TYPE] as number),
      },
      {
        header: 'Trạng thái',
        field: MON_HOC_KEY.STATUS,
        class: 'text-center',
        cellTemplate: this.statusTpl,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'visibility',
            class: 'action-view',
            tooltip: 'Chi tiết',
            click: (rowData: MonHocResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            tooltip: 'Chỉnh sửa',
            click: (rowData: MonHocResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            tooltip: 'Xóa',
            click: (rowData: MonHocResponse) => this.deleteMonHoc(rowData),
          },
        ],
      },
    ];

    this.filterData({ pageIndex: 0, pageSize: this.pageSize });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    const formValues = this.form.getRawValue();
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        subject: formValues[MON_HOC_KEY.NAME] ?? undefined,
        type: formValues[MON_HOC_KEY.TYPE] ?? undefined,
        status: formValues[MON_HOC_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.monHocService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách môn học',
          'Thất bại'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  toggleAdvancedFilters() {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: MonHocResponse) {
    this.dialog.componentDialog(
      DialogMonHocComponent,
      {
        width: '720px',
        data: {
          type,
          id: rowData?.[MON_HOC_KEY.ID],
          data: rowData,
        },
      },
      (result?: boolean) => {
        if (result) {
          this.filterData({
            pageIndex: this.pageIndex,
            pageSize: this.pageSize,
          });
        }
      }
    );
  }

  deleteMonHoc(rowData: MonHocResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa môn học ${rowData[MON_HOC_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.monHocService.delete(rowData[MON_HOC_KEY.ID]).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.filterData({
              pageIndex: this.pageIndex,
              pageSize: this.pageSize,
            });
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

  exportPdf(): void {
    if (!this.canDownload) {
      this.toastr.warning('Bạn không có quyền tải xuống', 'Cảnh báo');
      return;
    }
    this.exportFile('PDF');
  }

  exportExcel(): void {
    if (!this.canDownload) {
      this.toastr.warning('Bạn không có quyền tải xuống', 'Cảnh báo');
      return;
    }
    this.exportFile('EXCEL');
  }

  import(): void {
    if (!this.canAdd) {
      this.toastr.warning('Bạn không có quyền kết nạp dữ liệu', 'Cảnh báo');
      return;
    }

    this.dialog.componentDialog(
      DialogImportMonHocComponent,
      {
        width: '900px',
      },
      (result) => {
        if (result) {
          this.resetFilter();
        }
      }
    );
  }

  getTypeLabel(type?: number) {
    return (
      MON_HOC_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? '--'
    );
  }

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const formValues = this.form.getRawValue();
    const payload: MonHocExportRequest = {
      pageSize: this.pageSize,
      pageNow: this.pageIndex + 1,
      exportType,
      filter: {
        subject: formValues[MON_HOC_KEY.NAME],
        type: formValues[MON_HOC_KEY.TYPE],
        status: formValues[MON_HOC_KEY.STATUS],
      },
    };

    this.monHocService.export(payload).subscribe({
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
        const fallbackName = defaultExportFileName('mon-hoc', ext);
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
}
