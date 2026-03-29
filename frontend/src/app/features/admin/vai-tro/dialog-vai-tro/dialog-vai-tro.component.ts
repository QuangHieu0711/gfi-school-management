import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  VAI_TRO_FORM,
  VAI_TRO_KEY,
  VaiTroFormRequest,
  VaiTroResponse,
} from '@app/model/admin/vai-tro.model';
import { VaiTroService } from '@app/service/admin/vai-tro.service';

@Component({
  selector: 'dialog-vai-tro',
  templateUrl: './dialog-vai-tro.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogVaiTroComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = VAI_TRO_FORM;
  key = VAI_TRO_KEY;
  title = '';
  currentData: VaiTroResponse | null = null;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogVaiTroComponent>,
    private readonly vaiTroService: VaiTroService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa vai trò';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết vai trò';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới vai trò';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      this.getDetail(this.data.id);
    }
  }

  private getDetail(id: ID_TYPE) {
    this.vaiTroService.getById(id).subscribe(({ data }) => {
      this.currentData = data;
      this.form.patchValue(data);
    });
  }

  onSubmit() {
    const payload = this.form.getRawValue() as VaiTroFormRequest;

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Chỉnh sửa vai trò';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private handleCreate(payload: VaiTroFormRequest) {
    this.vaiTroService.create(payload).subscribe({
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

  private handleUpdate(payload: VaiTroFormRequest) {
    this.vaiTroService.update(this.data.id!, payload).subscribe({
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
}
