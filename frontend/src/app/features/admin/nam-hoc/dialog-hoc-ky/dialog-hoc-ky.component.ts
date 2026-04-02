import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  HOC_KY_FORM,
  HOC_KY_KEY,
  HocKyFormRequest,
} from '@app/model/admin/hoc-ky.model';
import { HocKyService } from '@app/service/admin/hoc-ky.service';

@Component({
  selector: 'dialog-hoc-ky',
  templateUrl: './dialog-hoc-ky.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogHocKyComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = HOC_KY_FORM;
  key = HOC_KY_KEY;
  title = '';

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogHocKyComponent>,
    private readonly hocKyService: HocKyService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      id?: ID_TYPE;
      schoolYearId: ID_TYPE;
      schoolYearName?: string;
    } = {
      type: TYPE_FORM.CREATE,
      schoolYearId: 0,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa học kỳ';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết học kỳ';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới học kỳ';
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
    this.title = 'Chỉnh sửa học kỳ';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private getDetail(id: ID_TYPE) {
    this.hocKyService.getById(id).subscribe(({ data }) => {
      this.form.patchValue({
        ...data,
        [HOC_KY_KEY.START_DATE]: this.toFormDate(data?.startDate),
        [HOC_KY_KEY.END_DATE]: this.toFormDate(data?.endDate),
      });
    });
  }

  private buildPayload(): HocKyFormRequest | null {
    const value = this.form.getRawValue();
    const startDate = this.toApiDate(value[HOC_KY_KEY.START_DATE]);
    const endDate = this.toApiDate(value[HOC_KY_KEY.END_DATE]);

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
      [HOC_KY_KEY.SCHOOL_YEAR_ID]: this.data.schoolYearId,
      [HOC_KY_KEY.CODE]: value[HOC_KY_KEY.CODE],
      [HOC_KY_KEY.NAME]: value[HOC_KY_KEY.NAME],
      [HOC_KY_KEY.SEMESTER_ORDER]: Number(value[HOC_KY_KEY.SEMESTER_ORDER]),
      [HOC_KY_KEY.START_DATE]: startDate,
      [HOC_KY_KEY.END_DATE]: endDate,
      [HOC_KY_KEY.STATUS]: value[HOC_KY_KEY.STATUS],
      [HOC_KY_KEY.IS_CURRENT]: value[HOC_KY_KEY.IS_CURRENT],
      [HOC_KY_KEY.DESCRIPTION]: value[HOC_KY_KEY.DESCRIPTION],
    };
  }

  private handleCreate(payload: HocKyFormRequest) {
    this.hocKyService.create(payload).subscribe({
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

  private handleUpdate(payload: HocKyFormRequest) {
    this.hocKyService.update(this.data.id!, payload).subscribe({
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
