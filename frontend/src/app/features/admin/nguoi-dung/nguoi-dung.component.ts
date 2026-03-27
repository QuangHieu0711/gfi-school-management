/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { selectMatBtn } from '@store/style/selectors';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { AuthService } from '@service';
import {
  FormType,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import {
  NGUOI_DUNG_KEY,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { DialogThemNguoiDungComponent } from './dialog-them-nguoi-dung/dialog-them-nguoi-dung.component';
import { NguoiDungService } from '@app/service/admin/nguoi-dung.service';
import { filter, take } from 'rxjs';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { defaultExportFileName, saveBlobAsFile } from '@utils/file-util';
import { DialogImportComponent } from './dialog-import/dialog-import.component';
import { PermissionService } from './../../../../lib/core/services/permission.service';

@Component({
  selector: 'nguoi-dung',
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
  @ViewChild('tenTaiKhoanTpl', { static: true })
  tenTaiKhoanTpl!: TemplateRef<unknown>;
  permissionUrl = '/Admin/NguoiDung';
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem: FormType[] = [
    TEXT_CONTROL({
      controlName: NGUOI_DUNG_KEY.TEN_NGUOI_DUNG,
      placeholder: 'Tìm kiếm theo tên người dùng hoặc tên tài khoản hoặc email',
      required: false,
      maxLength: 255,
    }),
    SELECT_CONTROL({
      controlName: NGUOI_DUNG_KEY.UNITID,
      placeholder: 'Đơn vị',
      required: false,
      clearable: true,
      multiple: true,
      maskCount: 2,
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

  constructor(
    protected override injector: Injector,
    private readonly nguoiDungService: NguoiDungService,
    private readonly authService: AuthService,
    public permission: PermissionService
  ) {
    super(injector);
    this.matButton = this.store.selectSignal(selectMatBtn) as any;
  }

  matButton: any;

  // ===== helpers (Copy chuẩn từ Đơn vị) =====
  private extractBlob(res: any): Blob | null {
    if (res instanceof Blob) return res;
    if (res?.body instanceof Blob) return res.body;
    if (res?.data instanceof Blob) return res.data;
    return null;
  }

  private getHeader(res: any, headerName: string): string | null {
    if (res?.headers?.get) return res.headers.get(headerName);

    const h = res?.headers;
    if (h && typeof h === 'object') {
      const key = headerName.toLowerCase();
      return h[headerName] ?? h[key] ?? null;
    }
    return null;
  }

  private getFileNameFromDisposition(
    disposition: string | null,
    fallbackName: string
  ): string {
    if (!disposition) return fallbackName;

    const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)/i);
    if (match?.[1]) {
      try {
        return decodeURIComponent(match[1]);
      } catch {
        return match[1];
      }
    }
    return fallbackName;
  }

  private buildExportPayload(exportType: 'PDF' | 'EXCEL') {
    const formValues = this.form.getRawValue();

    return {
      pageSize: this.pageSize,
      pageNow: this.pageIndex + 1,
      filter: {
        userName: formValues[NGUOI_DUNG_KEY.TEN_NGUOI_DUNG] ?? undefined,
        unitId: formValues[NGUOI_DUNG_KEY.UNITID] ?? undefined,
      },
      username: formValues[NGUOI_DUNG_KEY.TEN_NGUOI_DUNG] ?? undefined,
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

  // ===== public methods =====
  exportPdf(): void {
    this.exportFile('PDF');
  }

  exportExcel(): void {
    this.exportFile('EXCEL');
  }

  protected override componentInit(): void {
    this.permission.rules$
      .pipe(
        filter((rules) => rules.length > 0),
        take(1)
      )
      .subscribe();

    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Tên người dùng',
        field: NGUOI_DUNG_KEY.TEN_NGUOI_DUNG,
        formatter: (rowData: NguoiDungResponse) =>
          [rowData?.[NGUOI_DUNG_KEY.HO], rowData?.[NGUOI_DUNG_KEY.TEN]]
            .filter((txt) => txt)
            .join(' '),
      },
      {
        header: 'Tên tài khoản',
        field: NGUOI_DUNG_KEY.TEN_TAI_KHOAN,
        cellTemplate: this.tenTaiKhoanTpl,
      },
      {
        header: 'Email',
        field: NGUOI_DUNG_KEY.EMAIL,
      },
      {
        header: 'Đơn vị',
        field: NGUOI_DUNG_KEY.TEN_DON_VI,
      },
      {
        header: 'Trạng thái',
        field: NGUOI_DUNG_KEY.STATUS,
        formatter: (rowData: NguoiDungResponse) =>
          rowData?.[NGUOI_DUNG_KEY.STATUS] === 0
            ? 'Đang hoạt động'
            : 'Không hoạt động',
      },
      {
        header: 'Nhóm quyền',
        field: NGUOI_DUNG_KEY.NHOM_QUYEN_NAME,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        buttons: [
          {
            type: 'icon',
            icon: 'key',
            tooltip: 'Mở khóa / Reset mật khẩu',
            click: (rowData: NguoiDungResponse) => {
              this.dialog.confirm(
                {
                  title: 'Xác nhận',
                  message: `Bạn có chắc chắn muốn reset mật khẩu cho tài khoản ${rowData[NGUOI_DUNG_KEY.TEN_TAI_KHOAN]} không?`,
                },
                (confirmed) => {
                  if (confirmed) {
                    this.authService
                      .resetPassword(rowData?.[NGUOI_DUNG_KEY.ID])
                      .subscribe({
                        next: () => {
                          this.toastr.success(
                            'Reset mật khẩu thành công',
                            'Thành công'
                          );
                        },
                        error: (err) => {
                          this.toastr.error(err.message, 'Lỗi');
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
            tooltip: 'Chỉnh sửa',
            iif: () => this.permission.canEdit(this.permissionUrl),
            click: (rowData: NguoiDungResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            tooltip: 'Xóa',
            iif: () => this.permission.canDelete(this.permissionUrl),
            click: (rowData: NguoiDungResponse) =>
              this.dialog.confirm(
                {
                  message: `Bạn có chắc chắn muốn xóa ${rowData[NGUOI_DUNG_KEY.TEN_TAI_KHOAN]} không?`,
                  title: 'Xác nhận',
                },
                (confirmed?: boolean) => {
                  if (confirmed) {
                    this.nguoiDungService
                      .delete([rowData?.[NGUOI_DUNG_KEY.ID]])
                      .subscribe({
                        next: () => {
                          this.toastr.success('Xóa thành công', 'Thành công');
                          // Lùi trang nếu xóa bản ghi cuối cùng của trang hiện tại (giống Đơn vị)
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
                              `Xóa ${rowData[NGUOI_DUNG_KEY.TEN_NGUOI_DUNG]} thất bại do có dữ liệu phụ thuộc`,
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

  filterData(pageChangeEvent?: TableQueryEvent) {
    const formValues = this.form.getRawValue();
    const filterValue = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: pageChangeEvent?.pageIndex ?? 0,
      filter: {
        unitId: formValues[NGUOI_DUNG_KEY.UNITID],
        name: formValues[NGUOI_DUNG_KEY.TEN_NGUOI_DUNG],
        status: formValues[NGUOI_DUNG_KEY.STATUS],
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.nguoiDungService.filter(filterValue).subscribe(({ data }) => {
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
          data: rowData,
          id: rowData?.[NGUOI_DUNG_KEY.ID],
        },
      },
      (result?: boolean) => {
        if (result) {
          if (type === TYPE_FORM.CREATE) this.resetFilter();
          else
            this.filterData({
              pageIndex: this.pageIndex,
              pageSize: this.pageSize,
            });
        }
      }
    );
  }

  import() {
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
}
