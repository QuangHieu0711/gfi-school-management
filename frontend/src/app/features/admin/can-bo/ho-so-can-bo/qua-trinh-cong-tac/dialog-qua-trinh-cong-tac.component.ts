import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType, IOptions } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  STAFF_JOB_HISTORY_FORM,
  STAFF_JOB_HISTORY_KEY,
  StaffJobHistoryFormRequest,
  StaffJobHistoryResponse,
} from '@app/model/admin/qua-trinh-cong-tac.model';
import { StaffJobHistoryService } from '@app/service/admin/qua-trinh-cong-tac.service';

@Component({
  selector: 'dialog-qua-trinh-cong-tac',
  standalone: true,
  templateUrl: './dialog-qua-trinh-cong-tac.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogQuaTrinhCongTacComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly key = STAFF_JOB_HISTORY_KEY;
  title = '';
  $formItem: FormType[] = structuredClone(STAFF_JOB_HISTORY_FORM) as FormType[];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogQuaTrinhCongTacComponent>,
    private readonly staffJobHistoryService: StaffJobHistoryService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      staffId: ID_TYPE;
      id?: ID_TYPE;
      data?: StaffJobHistoryResponse;
      unitOptions?: IOptions[];
    }
  ) {
    super(injector);
    this.findFormControl(this.$formItem, this.key.UNIT_ID).options =
      data.unitOptions ?? [];
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa quá trình công tác';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết quá trình công tác';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới quá trình công tác';
        break;
    }

    if (this.data.data) {
      this.patchData(this.data.data);
    } else if (
      this.data.id != null &&
      this.data.type !== this.TYPE_FORM.CREATE
    ) {
      this.staffJobHistoryService
        .getById(this.data.id)
        .subscribe(({ data }) => {
          this.patchData(data);
        });
    }
  }

  onSubmit(): void {
    const payload = this.buildPayload();

    if (this.data.type === this.TYPE_FORM.CREATE) {
      this.staffJobHistoryService.create(payload).subscribe({
        next: () => {
          this.toastr.success('Lưu thành công', 'Thành công');
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Lưu thất bại',
            'Thất bại'
          );
        },
      });
      return;
    }

    this.staffJobHistoryService.update(this.data.id!, payload).subscribe({
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

  switchUpdate(): void {
    this.form.enable();
    this.title = 'Chỉnh sửa quá trình công tác';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private patchData(data: StaffJobHistoryResponse): void {
    this.form.patchValue(
      {
        fromDate: data.fromDate?.slice(0, 10) ?? '',
        toDate: data.toDate?.slice(0, 10) ?? '',
        unitId: data.unitId ?? '',
        departmentId: data.departmentId ?? '',
        workingPositionId: data.workingPositionId ?? '',
        titleId: data.titleId ?? '',
        employmentTypeId: data.employmentTypeId ?? '',
        decisionNo: data.decisionNo ?? '',
        note: data.note ?? '',
      },
      { emitEvent: false }
    );
  }

  private buildPayload(): StaffJobHistoryFormRequest {
    const value = this.form.getRawValue();
    return {
      staffId: this.data.staffId,
      fromDate: value.fromDate ?? '',
      toDate: value.toDate ?? '',
      unitId: value.unitId ?? '',
      departmentId: value.departmentId ?? '',
      workingPositionId: value.workingPositionId ?? '',
      titleId: value.titleId ?? '',
      employmentTypeId: value.employmentTypeId ?? '',
      decisionNo: value.decisionNo ?? '',
      note: value.note ?? '',
    };
  }
}
