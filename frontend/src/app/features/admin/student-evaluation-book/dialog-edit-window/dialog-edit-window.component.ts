import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { ComponentBaseAbstract } from '@layout';
import { DATE_CONTROL, FormType } from '@model/form-control.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  EvaluationEditWindowRequest,
  EvaluationEditWindowResponse,
} from '@app/model/admin/evaluation.model';
import { EvaluationService } from '@app/service/admin/evaluation.service';

const EDIT_WINDOW_KEY = {
  START_DATE: 'startDate',
  END_DATE: 'endDate',
} as const;

@Component({
  selector: 'dialog-edit-window',
  standalone: true,
  templateUrl: './dialog-edit-window.component.html',
  styleUrls: ['./dialog-edit-window.component.scss'],
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogEditWindowComponent extends ComponentBaseAbstract {
  readonly key = EDIT_WINDOW_KEY;
  readonly title = 'Cấu hình thời gian sửa điểm';
  readonly $formItem: FormType[] = [
    DATE_CONTROL({
      controlName: this.key.START_DATE,
      label: 'Từ ngày',
      placeholder: 'Từ ngày',
      required: true,
      dateType: 'date',
    }),
    DATE_CONTROL({
      controlName: this.key.END_DATE,
      label: 'Đến ngày',
      placeholder: 'Đến ngày',
      required: true,
      dateType: 'date',
    }),
  ];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogEditWindowComponent>,
    private readonly evaluationService: EvaluationService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      semesterId: number;
      semesterName: string;
      currentConfig?: EvaluationEditWindowResponse | null;
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    this.form.patchValue(
      {
        [this.key.START_DATE]: this.toFormDate(this.data.currentConfig?.startDate),
        [this.key.END_DATE]: this.toFormDate(this.data.currentConfig?.endDate),
      },
      { emitEvent: false }
    );
  }

  onSubmit(): void {
    const rawValue = this.form.getRawValue();
    const payload: EvaluationEditWindowRequest = {
      semesterId: this.data.semesterId,
      startDate: this.toApiDate(rawValue[this.key.START_DATE]),
      endDate: this.toApiDate(rawValue[this.key.END_DATE]),
    };

    if (!payload.startDate || !payload.endDate) {
      this.toastr.error('Vui lòng chọn đầy đủ từ ngày và đến ngày', 'Thất bại');
      return;
    }

    if (payload.startDate > payload.endDate) {
      this.toastr.error(
        'Đến ngày phải lớn hơn hoặc bằng từ ngày',
        'Thất bại'
      );
      return;
    }

    this.evaluationService.saveEditWindow(payload).subscribe({
      next: () => {
        this.toastr.success(
          'Lưu cấu hình thời gian sửa điểm thành công',
          'Thành công'
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Lưu cấu hình thời gian sửa điểm thất bại',
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
