/* eslint-disable @typescript-eslint/no-explicit-any */
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
  DON_VI_FILTER_FORM,
  DON_VI_KEY,
  DonViExportRequest,
  DonViResponse,
} from '@app/model/admin/don-vi.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { PermissionCheckService } from '@service';
import { DialogDonViComponent } from './dialog-don-vi/dialog-don-vi.component';
import { DialogImportComponent } from './dialog-import/dialog-import.component';

@Component({
  selector: 'don-vi',
  templateUrl: './don-vi.component.html',
  styleUrls: ['./don-vi.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class DonViComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  tableConfig = {
    hasFilterPanel: true,
  };
  readonly menuCode = 'UNIT_MANAGEMENT';
  columns: MtxGridColumn[] = [];
  $formItem = DON_VI_FILTER_FORM;
  key = DON_VI_KEY;
  dataSource: DonViResponse[] = [];

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canDownload(): boolean {
    return this.permissionCheckService.canDownload(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly donViService: DonViService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Mã đơn vị',
        field: DON_VI_KEY.CODE,
      },
      {
        header: 'Tên đơn vị',
        field: DON_VI_KEY.NAME,
      },
      {
        header: 'Địa chỉ',
        field: DON_VI_KEY.ADDRESS,
      },
      {
        header: 'Trạng thái',
        field: DON_VI_KEY.STATUS,
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
            iif: () => this.permissionCheckService.canView(this.menuCode),
            click: (rowData: DonViResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            click: (rowData: DonViResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            click: (rowData: DonViResponse) => this.deleteUnit(rowData),
          },
        ],
      },
    ];

    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    const formValues = this.form.getRawValue();
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        unitName: formValues[DON_VI_KEY.NAME] ?? undefined,
        status: formValues[DON_VI_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.donViService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách đơn vị',
          'Thất bại'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: DonViResponse) {
    this.dialog.componentDialog(
      DialogDonViComponent,
      {
        width: '560px',
        data: {
          type,
          id: rowData?.[DON_VI_KEY.ID],
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

  deleteUnit(rowData: DonViResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa đơn vị ${rowData[DON_VI_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.donViService.delete(rowData[DON_VI_KEY.ID]).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');

            if (this.dataSource.length === 1 && this.pageIndex > 0) {
              this.pageIndex = this.pageIndex - 1;
            }

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

  import() {
    if (!this.canAdd) {
      this.toastr.warning('Bạn không có quyền kết nạp dữ liệu', 'Cảnh báo');
      return;
    }
    this.dialog.componentDialog(
      DialogImportComponent,
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

  downloadTemplate(): void {
    this.donViService.downloadTemplate().subscribe({
      next: (response: any) => {
        const blob = response?.data ?? response;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'template_import_danh_sach_don_vi.xlsx';
        a.click();
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
      },
      error: () => {
        this.toastr.error('Không tải được file mẫu', 'Thất bại');
      },
    });
  }

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const formValues = this.form.getRawValue();
    const payload: DonViExportRequest = {
      pageSize: this.pageSize,
      pageNow: this.pageIndex + 1,
      exportType,
      filter: {
        unitName: formValues[DON_VI_KEY.NAME],
        status: formValues[DON_VI_KEY.STATUS],
      },
    };

    this.donViService.export(payload).subscribe({
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
        const fallbackName = defaultExportFileName('don-vi', ext);
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
