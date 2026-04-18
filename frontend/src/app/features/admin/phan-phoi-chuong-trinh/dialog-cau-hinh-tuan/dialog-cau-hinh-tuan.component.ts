import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { ComponentBaseAbstract } from '@layout';
import { FormType } from '@model/form-control.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  WEEK_CONFIG_FORM,
  WEEK_CONFIG_KEY,
  WeekConfigResponse,
  WeekConfigUpdateRequest,
} from '@app/model/admin/week-config.model';
import { WeekConfigService } from '@app/service/admin/week-config.service';

@Component({
  selector: 'dialog-cau-hinh-tuan',
  standalone: true,
  templateUrl: './dialog-cau-hinh-tuan.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogCauHinhTuanComponent extends ComponentBaseAbstract {
  readonly key = WEEK_CONFIG_KEY;
  title = 'Chỉnh sửa cấu hình tuần';
  $formItem: FormType[] = structuredClone(WEEK_CONFIG_FORM);

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogCauHinhTuanComponent>,
    private readonly weekConfigService: WeekConfigService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      weekConfig: WeekConfigResponse;
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    this.form.patchValue({
      [this.key.WEEK_NUMBER]: this.data.weekConfig?.[this.key.WEEK_NUMBER],
      [this.key.START_DATE]: this.toFormDate(
        this.data.weekConfig?.[this.key.START_DATE]
      ),
      [this.key.END_DATE]: this.toFormDate(
        this.data.weekConfig?.[this.key.END_DATE]
      ),
    });

    this.form.get(this.key.WEEK_NUMBER)?.disable({ emitEvent: false });
  }

  onSubmit(): void {
    const id = this.data.weekConfig?.[this.key.ID];
    if (id == null) {
      this.toastr.error('Không xác định được bản ghi để cập nhật', 'Thất bại');
      return;
    }

    const value = this.form.getRawValue();
    const payload: WeekConfigUpdateRequest = {
      weekNumber: Number(this.data.weekConfig?.[this.key.WEEK_NUMBER] ?? 0),
      startDate: this.toApiDate(value[this.key.START_DATE]),
      endDate: this.toApiDate(value[this.key.END_DATE]),
    };

    if (!payload.startDate || !payload.endDate) {
      this.toastr.error(
        'Ngày bắt đầu và ngày kết thúc không hợp lệ',
        'Thất bại'
      );
      return;
    }

    if (payload.startDate > payload.endDate) {
      this.toastr.error(
        'Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu',
        'Thất bại'
      );
      return;
    }

    this.weekConfigService.update(id, payload).subscribe({
      next: () => {
        this.toastr.success(
          'Cập nhật tuần thành công, các tuần sau đã được tính lại tự động',
          'Thành công'
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Cập nhật cấu hình tuần thất bại',
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
