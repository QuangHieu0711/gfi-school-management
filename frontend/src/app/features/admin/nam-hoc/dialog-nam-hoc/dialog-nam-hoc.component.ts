import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  NAM_HOC_FORM,
  NAM_HOC_KEY,
  NamHocFormRequest,
} from '@app/model/admin/nam-hoc.model';
import { NamHocService } from '@app/service/admin/nam-hoc.service';

@Component({
  selector: 'dialog-nam-hoc',
  templateUrl: './dialog-nam-hoc.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogNamHocComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = NAM_HOC_FORM;
  key = NAM_HOC_KEY;
  title = '';

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogNamHocComponent>,
    private readonly namHocService: NamHocService,
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
        this.title = 'Chỉnh sửa năm học';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết năm học';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới năm học';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      this.getDetail(this.data.id);
    }
  }

  onSubmit() {
    const payload = this.buildPayload();
    if (!payload) return;

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Chỉnh sửa năm học';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private getDetail(id: ID_TYPE) {
    this.namHocService.getById(id).subscribe(({ data }) => {
      this.form.patchValue({
        ...data,
        [NAM_HOC_KEY.START_DATE]: this.toFormDate(data?.startDate),
        [NAM_HOC_KEY.END_DATE]: this.toFormDate(data?.endDate),
      });
    });
  }

  private buildPayload(): NamHocFormRequest | null {
    const value = this.form.getRawValue();
    const startDate = this.toApiDate(value[NAM_HOC_KEY.START_DATE]);
    const endDate = this.toApiDate(value[NAM_HOC_KEY.END_DATE]);

    if (!startDate || !endDate) {
      this.toastr.error('Ngày bắt đầu và ngày kết thúc không hợp lệ', 'Thất bại');
      return null;
    }

    if (startDate > endDate) {
      this.toastr.error(
        'Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu',
        'Thất bại'
      );
      return null;
    }

    return {
      [NAM_HOC_KEY.CODE]: value[NAM_HOC_KEY.CODE],
      [NAM_HOC_KEY.NAME]: value[NAM_HOC_KEY.NAME],
      [NAM_HOC_KEY.START_DATE]: startDate,
      [NAM_HOC_KEY.END_DATE]: endDate,
      [NAM_HOC_KEY.STATUS]: value[NAM_HOC_KEY.STATUS],
      [NAM_HOC_KEY.IS_CURRENT]: value[NAM_HOC_KEY.IS_CURRENT],
      [NAM_HOC_KEY.DESCRIPTION]: value[NAM_HOC_KEY.DESCRIPTION],
    };
  }

  private handleCreate(payload: NamHocFormRequest) {
    this.namHocService.create(payload).subscribe({
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

  private handleUpdate(payload: NamHocFormRequest) {
    this.namHocService.update(this.data.id!, payload).subscribe({
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

  private toFormDate(value?: string | null): string | null {
    if (!value) return null;
    return value.includes('T') ? value : `${value}T00:00:00`;
  }

  private toApiDate(value: unknown): string {
    if (!value) return '';
    if (value instanceof Date) {
      return value.toISOString().slice(0, 10);
    }

    return String(value).slice(0, 10);
  }
}
