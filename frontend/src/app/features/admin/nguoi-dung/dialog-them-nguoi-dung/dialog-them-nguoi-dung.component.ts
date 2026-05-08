import { HttpErrorResponse } from '@angular/common/http';
import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { sha256 } from '@utils/utils';

import {
  NGUOI_DUNG_FORM,
  NGUOI_DUNG_KEY,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { NguoiDungService } from '@app/service/admin/nguoi-dung.service';
import { PermissionService } from '../../../../../lib/core/services/permission.service';

@Component({
  selector: 'dialog-them-nguoi-dung',
  templateUrl: './dialog-them-nguoi-dung.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogThemNguoiDungComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = NGUOI_DUNG_FORM();
  key = NGUOI_DUNG_KEY;
  permissionUrl = '/Admin/NguoiDung';
  title = '';
  currentData: NguoiDungResponse | null = null;
  staffOptionsData: any[] = [];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogThemNguoiDungComponent>,
    private readonly nguoiDungService: NguoiDungService,
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
    // Only load options when creating or updating
    if (
      this.data.type === this.TYPE_FORM.CREATE ||
      this.data.type === this.TYPE_FORM.UPDATE
    ) {
      this.nguoiDungService.getCreateUserRoleOptions().subscribe(({ data }) => {
        this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.ROLE_ID).options = (
          data ?? []
        ).map((item) => ({
          value: item.id,
          label: item.name,
        }));
      });

      this.nguoiDungService.getStaffOptions().subscribe(({ data }) => {
        this.staffOptionsData = data ?? [];
        const staffControl = this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.STAFF_ID);
        if (staffControl) {
          staffControl.options = this.staffOptionsData.map((item) => ({
            value: item.id,
            label: item.name,
          }));
        }
      });

      this.form.get(NGUOI_DUNG_KEY.STAFF_ID)?.valueChanges.subscribe((staffId) => {
        const staff = this.staffOptionsData.find(s => s.id === staffId);
        if (staff) {
          this.form.patchValue({
            [NGUOI_DUNG_KEY.STAFF_CODE]: staff.staffCode,
            [NGUOI_DUNG_KEY.STAFF_EMAIL]: staff.email,
            [NGUOI_DUNG_KEY.STAFF_UNIT]: staff.unitName
          }, { emitEvent: false });
        } else {
          this.form.patchValue({
            [NGUOI_DUNG_KEY.STAFF_CODE]: null,
            [NGUOI_DUNG_KEY.STAFF_EMAIL]: null,
            [NGUOI_DUNG_KEY.STAFF_UNIT]: null
          }, { emitEvent: false });
        }
      });
    }

    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.form.get(NGUOI_DUNG_KEY.USERNAME)?.disable();
        this.form.get(NGUOI_DUNG_KEY.PASSWORD)?.disable();
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

      const fullName = data[NGUOI_DUNG_KEY.FULL_NAME] ?? '';
      this.title =
        this.data.type === this.TYPE_FORM.UPDATE
          ? `Chỉnh sửa tài khoản: ${fullName}`
          : fullName;
    });
  }

  async onSubmit() {
    const formValue = this.form.getRawValue();
    const payload: NguoiDungFormRequest = {
      [NGUOI_DUNG_KEY.USERNAME]: formValue[NGUOI_DUNG_KEY.USERNAME],
      [NGUOI_DUNG_KEY.ROLE_ID]: formValue[NGUOI_DUNG_KEY.ROLE_ID],
      [NGUOI_DUNG_KEY.STATUS]: formValue[NGUOI_DUNG_KEY.STATUS],
      [NGUOI_DUNG_KEY.STAFF_ID]: formValue[NGUOI_DUNG_KEY.STAFF_ID] || null,
    };

    if (this.data.type === this.TYPE_FORM.CREATE) {
      payload[NGUOI_DUNG_KEY.PASSWORD] = await sha256(
        formValue[NGUOI_DUNG_KEY.PASSWORD]
      );
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate({
      [NGUOI_DUNG_KEY.ID]: this.data.id,
      ...payload,
    });
  }

  handleCreate(payload: NguoiDungFormRequest) {
    this.nguoiDungService.create(payload).subscribe({
      next: () => {
        this.toastr.success('Lưu thành công!', 'Thành công');
        this.dialogRef.close(true);
        this.form.reset();
      },
      error: (error) => {
        this.toastr.error(
          error.error?.userMessage ?? 'Có lỗi xảy ra',
          'Thất bại'
        );
      },
    });
  }

  handleUpdate(payload: NguoiDungFormRequest) {
    this.nguoiDungService.update(payload).subscribe({
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
    this.form.get(NGUOI_DUNG_KEY.PASSWORD)?.disable();
    const fullName = this.currentData?.[NGUOI_DUNG_KEY.FULL_NAME] ?? '';
    this.title = `Chỉnh sửa tài khoản: ${fullName}`;
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  onPasswordMouseEnter() {
    this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.PASSWORD).hint =
      'Tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, ký tự số và ký tự đặc biệt';
  }

  onPasswordMouseLeave() {
    this.findFormControl(this.$formItem, NGUOI_DUNG_KEY.PASSWORD).hint = '';
  }
}
