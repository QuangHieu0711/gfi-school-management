/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { selectMatBtn } from '@store/style/selectors';
import { filter, take } from 'rxjs';

import { AuthService } from '@service';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import {
  FormType,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { IRule } from '@model/auth.model';
import { defaultExportFileName, saveBlobAsFile } from '@utils/file-util';

import {
  NGUOI_DUNG_KEY,
  NguoiDungFilterRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { NguoiDungService } from '@app/service/admin/nguoi-dung.service';
import { VaiTroService } from '@app/service/admin/vai-tro.service';
import { DialogImportComponent } from './dialog-import/dialog-import.component';
import { DialogThemNguoiDungComponent } from './dialog-them-nguoi-dung/dialog-them-nguoi-dung.component';
import { PermissionCheckService, PermissionService } from '@service';

@Component({
  selector: 'nguoi-dung',
  standalone: true,
  templateUrl: './nguoi-dung.component.html',
  styleUrls: ['./nguoi-dung.component.scss'],
  imports: [
    AppTableComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
    IconComponent,
  ],
})
export class NguoiDungComponent extends ComponentBaseAbstract {
  @ViewChild('usernameTpl', { static: true })
  usernameTpl!: TemplateRef<unknown>;
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  permissionUrl = '/Admin/NguoiDung';
  readonly menuCode = 'ACCOUNT_MANAGEMENT';
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem: FormType[] = [
    TEXT_CONTROL({
      controlName: NGUOI_DUNG_KEY.FULL_NAME,
      placeholder: 'Tìm kiếm theo họ tên hoặc tên tài khoản hoặc email',
      required: false,
      maxLength: 255,
    }),
    SELECT_CONTROL({
      controlName: NGUOI_DUNG_KEY.UNIT_ID,
      placeholder: 'Đơn vị',
      required: false,
      clearable: true,
      multiple: true,
      maskCount: 2,
    }),
    SELECT_CONTROL({
      controlName: NGUOI_DUNG_KEY.ROLE_ID,
      placeholder: 'Vai trò',
      required: false,
      clearable: true,
    }),
    SELECT_CONTROL({
      controlName: NGUOI_DUNG_KEY.STATUS,
      placeholder: 'Trạng thái',
      required: false,
      clearable: true,
      maskCount: 2,
      listOption: [
        { value: 0, label: 'Không hoạt động' },
        { value: 1, label: 'Đang hoạt động' },
      ],
    }),
  ];
  key = NGUOI_DUNG_KEY;
  dataSource: NguoiDungResponse[] = [];
  matButton: any;

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canDownload(): boolean {
    return this.permissionCheckService.canDownload(this.menuCode);
  }

  get hasAllScope(): boolean {
    return this.permissionCheckService.hasScope(this.menuCode, 'ALL');
  }

  get hasUnitScope(): boolean {
    return this.permissionCheckService.hasScope(this.menuCode, 'UNIT');
  }

  get allowedUnitIds(): number[] {
    return this.permissionCheckService.getAllowedScopeValues(
      this.menuCode,
      'UNIT'
    );
  }

  constructor(
    protected override injector: Injector,
    private readonly nguoiDungService: NguoiDungService,
    private readonly donViService: DonViService,
    private readonly vaiTroService: VaiTroService,
    private readonly authService: AuthService,
    private readonly permissionCheckService: PermissionCheckService,
    public permission: PermissionService
  ) {
    super(injector);
    this.matButton = this.store.selectSignal(selectMatBtn) as any;
  }

  protected override componentInit(): void {
    this.permission.rules$
      .pipe(
        filter((rules: IRule[]) => rules.length > 0),
        take(1)
      )
      .subscribe();

    this.form = this.itemControl.toFormGroup(this.$formItem);

    this.donViService.getOptions().subscribe(({ data }) => {
      let options = (data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));

      if (!this.hasAllScope && this.hasUnitScope) {
        options = options.filter((x) =>
          this.allowedUnitIds.includes(Number(x.value))
        );
      }

      const control = this.findFormControl(
        this.$formItem,
        NGUOI_DUNG_KEY.UNIT_ID
      );
      control.options = options;

      if (!this.hasAllScope && this.hasUnitScope) {
        this.form.patchValue({
          [NGUOI_DUNG_KEY.UNIT_ID]: this.allowedUnitIds,
        });
        control.disabled = true;
      }
    });

    this.vaiTroService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.ROLE_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Họ tên',
        field: NGUOI_DUNG_KEY.FULL_NAME,
      },
      {
        header: 'Tên tài khoản',
        field: NGUOI_DUNG_KEY.USERNAME,
        cellTemplate: this.usernameTpl,
      },
      {
        header: 'Email',
        field: NGUOI_DUNG_KEY.EMAIL,
      },
      {
        header: 'Đơn vị',
        field: NGUOI_DUNG_KEY.UNIT_NAME,
      },
      {
        header: 'Trạng thái',
        field: NGUOI_DUNG_KEY.STATUS,
        cellTemplate: this.statusTpl,
      },
      {
        header: 'Vai trò',
        field: NGUOI_DUNG_KEY.ROLE_NAME,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'key',
            class: 'action-view',
            tooltip: 'Mở khóa / Reset mật khẩu',
            iif: () => this.permissionCheckService.canConfig(this.menuCode),
            click: (rowData: NguoiDungResponse) => {
              this.dialog.confirm(
                {
                  title: 'Xác nhận',
                  message: `Bạn có chắc chắn muốn reset mật khẩu cho tài khoản ${rowData[NGUOI_DUNG_KEY.USERNAME]} không?`,
                },
                (confirmed) => {
                  if (confirmed) {
                    this.authService
                      .resetPassword(Number(rowData?.[NGUOI_DUNG_KEY.ID]))
                      .subscribe({
                        next: (res: any) => {
                          this.toastr.success(
                            res?.userMessage || 'Reset mật khẩu thành công',
                            'Thành công'
                          );
                        },
                        error: () => {
                          this.toastr.error('Có lỗi xảy ra', 'Lỗi');
                        },
                      });
                  }
                }
              );
            },
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            click: (rowData: NguoiDungResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            click: (rowData: NguoiDungResponse) =>
              this.dialog.confirm(
                {
                  message: `Bạn có chắc chắn muốn xóa ${rowData[NGUOI_DUNG_KEY.USERNAME]} không?`,
                  title: 'Xác nhận',
                },
                (confirmed?: boolean) => {
                  if (confirmed) {
                    this.nguoiDungService
                      .delete(Number(rowData?.[NGUOI_DUNG_KEY.ID]))
                      .subscribe({
                        next: () => {
                          this.toastr.success('Xóa thành công', 'Thành công');
                          if (
                            this.dataSource.length === 1 &&
                            this.pageIndex > 0
                          ) {
                            this.pageIndex = this.pageIndex - 1;
                          }
                          this.filterData();
                        },
                        error: ({ error }) => {
                          if (error.code === 3200) {
                            this.toastr.error(
                              `Xóa ${rowData[NGUOI_DUNG_KEY.USERNAME]} thất bại do có dữ liệu phụ thuộc`,
                              'Thất bại'
                            );
                            return;
                          }
                          this.toastr.error(error.message, 'Thất bại');
                        },
                      });
                  }
                }
              ),
          },
        ],
      },
    ];

    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
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

  filterData(pageChangeEvent?: TableQueryEvent) {
    const payload = this.buildFilterPayload(pageChangeEvent);

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.nguoiDungService.filter(payload).subscribe(({ data }) => {
      this.dataSource = data.items || [];
      this.dataSourceTotal = data.recordTotal || 0;
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: NguoiDungResponse) {
    this.dialog.componentDialog(
      DialogThemNguoiDungComponent,
      {
        width: '600px',
        data: {
          type,
          id: rowData?.[NGUOI_DUNG_KEY.ID],
        },
      },
      (result?: boolean) => {
        if (result) {
          if (type === TYPE_FORM.CREATE) {
            this.resetFilter();
            return;
          }

          this.filterData({
            pageIndex: this.pageIndex,
            pageSize: this.pageSize,
          });
        }
      }
    );
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

  private buildFilterPayload(
    pageChangeEvent?: TableQueryEvent
  ): NguoiDungFilterRequest {
    const formValues = this.form.getRawValue();

    const payload: NguoiDungFilterRequest = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? this.pageIndex) + 1,
      filter: {
        [NGUOI_DUNG_KEY.UNIT_ID]: formValues[NGUOI_DUNG_KEY.UNIT_ID],
        [NGUOI_DUNG_KEY.ROLE_ID]: formValues[NGUOI_DUNG_KEY.ROLE_ID],
        [NGUOI_DUNG_KEY.FULL_NAME]: formValues[NGUOI_DUNG_KEY.FULL_NAME],
        [NGUOI_DUNG_KEY.STATUS]: formValues[NGUOI_DUNG_KEY.STATUS],
      },
    };

    if (!this.hasAllScope && this.hasUnitScope) {
      payload.filter![NGUOI_DUNG_KEY.UNIT_ID] = this.allowedUnitIds;
    }

    return payload;
  }

  private buildExportPayload(exportType: 'PDF' | 'EXCEL') {
    return {
      ...this.buildFilterPayload({
        pageIndex: this.pageIndex,
        pageSize: this.pageSize,
      }),
      exportType,
    };
  }

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const payload = this.buildExportPayload(exportType);

    this.nguoiDungService.export(payload).subscribe({
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
        const fallbackName = defaultExportFileName('nguoi-dung', ext);
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
