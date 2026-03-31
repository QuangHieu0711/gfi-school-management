/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Inject, Injector } from '@angular/core';
import { FormType } from '@model/form-control.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import {
  NGUOI_DUNG_FORM,
  NGUOI_DUNG_KEY,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { DON_VI_KEY } from '@app/model/admin/don-vi.model';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DonViService } from '@app/service/admin/don-vi.service';
import { NguoiDungService } from '@app/service/admin/nguoi-dung.service';
import { ID_TYPE } from '@model/response.model';
import { sha256 } from '@utils/utils';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { PermissionService } from '../../../../../lib/core/services/permission.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'dialog-them-nguoi-dung',
  templateUrl: './dialog-them-nguoi-dung.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogThemNguoiDungComponent extends ComponentBaseAbstract {
  private readonly optionPageSize = 1000;
  private readonly firstPage = 1;

  $formItem: FormType[] = NGUOI_DUNG_FORM();
  key = NGUOI_DUNG_KEY;
  permissionUrl = '/Admin/NguoiDung';
  title = '';
  currentData: NguoiDungResponse | null = null;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogThemNguoiDungComponent>,
    private readonly nguoiDungService: NguoiDungService,
    private readonly donViService: DonViService,
    public permission: PermissionService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.$formItem = NGUOI_DUNG_FORM(this.data.type === this.TYPE_FORM.CREATE);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    this.donViService
      .filter({
        pageSize: this.optionPageSize,
        pageNow: this.firstPage,
        filter: {},
      })
      .subscribe(({ data }) => {
        const items = data.items ?? data.data ?? [];

        this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.UNITID).options =
          items.map((item) => ({
            value: item[DON_VI_KEY.CODE],
            label: `${item[DON_VI_KEY.CODE]} - ${item[DON_VI_KEY.NAME]}`,
          }));
      });

    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.form.get(NGUOI_DUNG_KEY.TEN_TAI_KHOAN)!.disable();
        this.form.get(NGUOI_DUNG_KEY.MAT_KHAU)!.disable();
        break;
      case this.TYPE_FORM.DETAIL:
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới tài khoản';
        break;
    }
    if (this.data.type !== this.TYPE_FORM.CREATE) {
      this.getDetail();
    }
  }

  getDetail() {
    this.nguoiDungService.getById(this.data.id!).subscribe(({ data }) => {
      this.currentData = data;
      this.form.patchValue(data);
      const hoTen =
        `${data[NGUOI_DUNG_KEY.HO] ?? ''} ${data[NGUOI_DUNG_KEY.TEN] ?? ''}`.trim();
      if (this.data.type === this.TYPE_FORM.UPDATE) {
        this.title = `Chỉnh sửa tài khoản: ${hoTen}`;
      } else {
        this.title = hoTen; // Chế độ Detail
      }
    });
  }

  async onSubmit() {
    const formValue = this.form.getRawValue();
    const payload = {
      ...formValue,
      [NGUOI_DUNG_KEY.MAT_KHAU]: await sha256(
        formValue[NGUOI_DUNG_KEY.MAT_KHAU]
      ),
    };

    if (this.data['type'] === this.TYPE_FORM.CREATE) this.handleCreate(payload);
    else this.handleUpdate(payload);
  }

  handleCreate(payload: NguoiDungFormRequest) {
    this.nguoiDungService
      .create(payload, { customError: { silent: true } })
      .subscribe({
        next: () => {
          this.toastr.success('Lưu thành công!', 'Thành công');
          this.dialogRef.close(true);
          this.form.reset();
        },
        error: (error) => {
          if (error.status === 500 && error.error.code === 3001)
            this.toastr.error(
              error.error?.userMessage ?? 'Có lỗi xảy ra',
              'Thất bại'
            );
          if (error.status === 500 && error.error.code === 3002)
            this.toastr.error(
              error.error?.userMessage ?? 'Có lỗi xảy ra',
              'Thất bại'
            );
          if (error.status === 400 && error.error.code === 1)
            this.toastr.error(
              error.error?.userMessage ?? 'Có lỗi xảy ra',
              'Thất bại'
            );
        },
      });
  }

  handleUpdate(payload: NguoiDungFormRequest) {
    this.nguoiDungService
      .update(
        {
          id: this.data.id,
          ...payload,
        },
        { customError: { silent: true } }
      )
      .subscribe({
        next: () => {
          this.toastr.success('Lưu thành công!', 'Thành công');
          this.dialogRef.close(true);
          this.form.reset();
        },
        error: (error: HttpErrorResponse) => {
          const message =
            error.error?.userMessage || error.message || 'Lưu dữ liệu thất bại';

          this.toastr.error(message, 'Thất bại');
        },
      });
  }

  switchUpdate() {
    this.form.enable();
    this.form.get(NGUOI_DUNG_KEY.MAT_KHAU)!.disable();
    const hoTen =
      `${this.currentData?.[NGUOI_DUNG_KEY.HO] ?? ''} ${this.currentData?.[NGUOI_DUNG_KEY.TEN] ?? ''}`.trim();
    this.title = `Chỉnh sửa tài khoản: ${hoTen}`;
    this.data['type'] = this.TYPE_FORM.UPDATE;
  }

  onPasswordMouseEnter() {
    this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.MAT_KHAU).hint =
      'Tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, ký tự số và ký tự đặc biệt';
  }

  onPasswordMouseLeave() {
    this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.MAT_KHAU).hint = '';
  }
}
