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
  LOP_FILTER_FORM,
  LOP_KEY,
  LopResponse,
} from '@app/model/admin/lop.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { KhoiService } from '@app/service/admin/khoi.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { DialogCauHinhMonHocLopComponent } from './dialog-cau-hinh-mon-hoc/dialog-cau-hinh-mon-hoc.component';
import { DialogLopComponent } from './dialog-lop/dialog-lop.component';
import { DialogImportComponent } from './dialog-import/dialog-import.component';
import { PermissionCheckService } from '@service';


@Component({
  selector: 'lop',
  templateUrl: './lop.component.html',
  styleUrls: ['./lop.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class LopComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'CLASS_MANAGEMENT';
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = LOP_FILTER_FORM;
  key = LOP_KEY;
  dataSource: LopResponse[] = [];
  showAdvancedFilters = false;

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canDownload(): boolean {
    return this.permissionCheckService.canDownload(this.menuCode);
  }


  constructor(
    protected override injector: Injector,
    private readonly lopService: LopService,
    private readonly donViService: DonViService,
    private readonly khoiService: KhoiService,
    private readonly namHocService: NamHocService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.loadSelectOptions();

    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Mã lớp',
        field: LOP_KEY.CODE,
      },
      {
        header: 'Tên lớp',
        field: LOP_KEY.NAME,
      },
      {
        header: 'Tên đơn vị',
        field: LOP_KEY.UNIT_NAME,
      },
      {
        header: 'Khối',
        field: LOP_KEY.GRADE_LEVEL_NAME,
      },
      {
        header: 'Năm học',
        field: LOP_KEY.SCHOOL_YEAR_NAME,
      },
      {
        header: 'Trạng thái',
        field: LOP_KEY.STATUS,
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
            click: (rowData: LopResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            tooltip: 'Chỉnh sửa',
            click: (rowData: LopResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'assignment',
            class: 'action-config',
            iif: () => this.permissionCheckService.canConfig(this.menuCode),
            tooltip: 'Cấu hình môn học',
            click: (rowData: LopResponse) => this.openSubjectConfig(rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            tooltip: 'Xóa',
            click: (rowData: LopResponse) => this.deleteLop(rowData),
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
        className: formValues[LOP_KEY.NAME] ?? undefined,
        unitId: formValues[LOP_KEY.UNIT_ID] ?? undefined,
        gradeLevelId: formValues[LOP_KEY.GRADE_LEVEL_ID] ?? undefined,
        schoolYearId: formValues[LOP_KEY.SCHOOL_YEAR_ID] ?? undefined,
        status: formValues[LOP_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.lopService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách lớp',
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

  openDialog(type: TYPE_FORM_KEY, rowData?: LopResponse) {
    this.dialog.componentDialog(
      DialogLopComponent,
      {
        width: '720px',
        data: {
          type,
          id: rowData?.[LOP_KEY.ID],
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

  openSubjectConfig(rowData: LopResponse) {
    this.dialog.componentDialog(DialogCauHinhMonHocLopComponent, {
      width: '980px',
      data: {
        classroomId: rowData[LOP_KEY.ID],
        classroom: rowData,
      },
    });
  }

  deleteLop(rowData: LopResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa lớp ${rowData[LOP_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.lopService.delete(rowData[LOP_KEY.ID]).subscribe({
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

  private loadSelectOptions() {
    this.donViService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.UNIT_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.khoiService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.GRADE_LEVEL_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.namHocService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.SCHOOL_YEAR_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });
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

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const formValues = this.form.getRawValue();
    const payload = {
      pageSize: this.pageSize,
      pageNow: this.pageIndex + 1,
      exportType,
      filter: {
        className: formValues[LOP_KEY.NAME] ?? undefined,
        unitId: formValues[LOP_KEY.UNIT_ID] ?? undefined,
        gradeLevelId: formValues[LOP_KEY.GRADE_LEVEL_ID] ?? undefined,
        schoolYearId: formValues[LOP_KEY.SCHOOL_YEAR_ID] ?? undefined,
        status: formValues[LOP_KEY.STATUS] ?? undefined,
      },
    };

    this.lopService.export(payload).subscribe({
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
        const fallbackName = defaultExportFileName('danh-sach-lop-hoc', ext);
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

