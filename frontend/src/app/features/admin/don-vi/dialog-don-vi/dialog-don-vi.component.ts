import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  DON_VI_FORM,
  DON_VI_KEY,
  DonViFormRequest,
  DonViResponse,
} from '@app/model/admin/don-vi.model';
import { DonViService } from '@app/service/admin/don-vi.service';

@Component({
  selector: 'dialog-don-vi',
  templateUrl: './dialog-don-vi.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogDonViComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = DON_VI_FORM;
  key = DON_VI_KEY;
  title = '';
  currentData: DonViResponse | null = null;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogDonViComponent>,
    private readonly donViService: DonViService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE; data?: DonViResponse } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa đơn vị';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết đơn vị';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới đơn vị';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      this.getDetail(this.data.id);
    }
  }

  onSubmit() {
    const payload = this.form.getRawValue() as DonViFormRequest;

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Chỉnh sửa đơn vị';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private handleCreate(payload: DonViFormRequest) {
    this.donViService.create(payload).subscribe({
      next: () => {
        this.toastr.success('Lưu thành công', 'Thành công');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu thất bại',
          'Thất bại'
        );
      },
    });
  }

  private handleUpdate(payload: DonViFormRequest) {
    this.donViService.update(this.data.id!, payload).subscribe({
      next: () => {
        this.toastr.success('Cập nhật thành công', 'Thành công');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Cập nhật thất bại',
          'Thất bại'
        );
      },
    });
  }

  private getDetail(id: ID_TYPE) {
    this.donViService.getById(id).subscribe(({ data }) => {
      this.currentData = data;
      this.form.patchValue(data);
    });
  }
}
