import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  STAFF_TRAINING_FORM,
  STAFF_TRAINING_KEY,
  StaffTrainingFormRequest,
  StaffTrainingResponse,
} from '@app/model/admin/dao-tao-can-bo.model';
import { StaffTrainingService } from '@app/service/admin/dao-tao-can-bo.service';

@Component({
  selector: 'dialog-thong-tin-dao-tao',
  standalone: true,
  templateUrl: './dialog-thong-tin-dao-tao.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogThongTinDaoTaoComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly key = STAFF_TRAINING_KEY;
  title = '';
  $formItem: FormType[] = structuredClone(STAFF_TRAINING_FORM) as FormType[];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogThongTinDaoTaoComponent>,
    private readonly staffTrainingService: StaffTrainingService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      staffId: ID_TYPE;
      id?: ID_TYPE;
      data?: StaffTrainingResponse;
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa thông tin đào tạo';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết thông tin đào tạo';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới thông tin đào tạo';
        break;
    }

    if (this.data.data) {
      this.patchData(this.data.data);
    } else if (
      this.data.id != null &&
      this.data.type !== this.TYPE_FORM.CREATE
    ) {
      this.staffTrainingService.getById(this.data.id).subscribe(({ data }) => {
        this.patchData(data);
      });
    }
  }

  onSubmit(): void {
    const payload = this.buildPayload();

    if (this.data.type === this.TYPE_FORM.CREATE) {
      this.staffTrainingService.create(payload).subscribe({
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
      return;
    }

    this.staffTrainingService.update(this.data.id!, payload).subscribe({
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
    this.title = 'Chỉnh sửa thông tin đào tạo';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private patchData(data: StaffTrainingResponse): void {
    this.form.patchValue(
      {
        schoolName: data.schoolName ?? '',
        major: data.major ?? '',
        trainingForm: data.trainingForm ?? '',
        certificate: data.certificate ?? '',
        fromDate: data.fromDate?.slice(0, 10) ?? '',
        toDate: data.toDate?.slice(0, 10) ?? '',
        note: data.note ?? '',
      },
      { emitEvent: false }
    );
  }

  private buildPayload(): StaffTrainingFormRequest {
    const value = this.form.getRawValue();

    return {
      staffId: this.data.staffId,
      schoolName: value.schoolName ?? '',
      major: value.major ?? '',
      trainingForm: value.trainingForm ?? '',
      certificate: value.certificate ?? '',
      fromDate: value.fromDate ?? '',
      toDate: value.toDate ?? '',
      note: value.note ?? '',
    };
  }
}
