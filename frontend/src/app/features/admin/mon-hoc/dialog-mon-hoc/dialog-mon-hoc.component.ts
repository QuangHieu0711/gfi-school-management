import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  MON_HOC_FORM,
  MON_HOC_KEY,
  MonHocFormRequest,
  MonHocResponse,
} from '@app/model/admin/mon-hoc.model';
import { MonHocService } from '@app/service/admin/mon-hoc.service';

@Component({
  selector: 'dialog-mon-hoc',
  templateUrl: './dialog-mon-hoc.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogMonHocComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = MON_HOC_FORM;
  key = MON_HOC_KEY;
  title = '';

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogMonHocComponent>,
    private readonly monHocService: MonHocService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE; data?: MonHocResponse } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa môn học';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết môn học';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới môn học';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      this.getDetail(this.data.id);
    }
  }

  onSubmit() {
    const rawValue = this.form.getRawValue();
    const payload: MonHocFormRequest = {
      [MON_HOC_KEY.CODE]: rawValue[MON_HOC_KEY.CODE],
      [MON_HOC_KEY.NAME]: rawValue[MON_HOC_KEY.NAME],
      [MON_HOC_KEY.TYPE]: Number(rawValue[MON_HOC_KEY.TYPE]),
      [MON_HOC_KEY.DESCRIPTION]: rawValue[MON_HOC_KEY.DESCRIPTION],
      [MON_HOC_KEY.STATUS]: Number(rawValue[MON_HOC_KEY.STATUS]),
    };

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Chỉnh sửa môn học';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private handleCreate(payload: MonHocFormRequest) {
    this.monHocService.create(payload).subscribe({
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

  private handleUpdate(payload: MonHocFormRequest) {
    this.monHocService.update(this.data.id!, payload).subscribe({
      next: () => {
        this.toastr.success('Cập nhật thành công', 'Thành công');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Cập nhật thất bại',
          'Thất bại'
        );
      },
    });
  }

  private getDetail(id: ID_TYPE) {
    this.monHocService.getById(id).subscribe(({ data }) => {
      this.form.patchValue(data);
    });
  }
}
