import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  PHAN_PHOI_CHUONG_TRINH_FORM,
  PHAN_PHOI_CHUONG_TRINH_KEY,
  PhanPhoiChuongTrinhFormRequest,
  PhanPhoiChuongTrinhResponse,
} from '@app/model/admin/phan-phoi-chuong-trinh.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { MonHocOptionResponse } from '@app/model/admin/mon-hoc.model';
import { LopService } from '@app/service/admin/lop.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { PhanPhoiChuongTrinhService } from '@app/service/admin/phan-phoi-chuong-trinh.service';

@Component({
  selector: 'dialog-phan-phoi-chuong-trinh',
  standalone: true,
  templateUrl: './dialog-phan-phoi-chuong-trinh.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogPhanPhoiChuongTrinhComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = structuredClone(
    PHAN_PHOI_CHUONG_TRINH_FORM
  ) as FormType[];
  key = PHAN_PHOI_CHUONG_TRINH_KEY;
  title = '';

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogPhanPhoiChuongTrinhComponent>,
    private readonly phanPhoiChuongTrinhService: PhanPhoiChuongTrinhService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      id?: ID_TYPE;
      data?: PhanPhoiChuongTrinhResponse;
    } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa phân phối chương trình';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết phân phối chương trình';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới phân phối chương trình';
        break;
    }

    this.loadOptions();

    if (this.data.data) {
      this.form.patchValue(this.data.data);
    } else if (
      this.data.type !== this.TYPE_FORM.CREATE &&
      this.data.id != null
    ) {
      this.phanPhoiChuongTrinhService
        .getById(this.data.id)
        .subscribe(({ data }) => {
          this.form.patchValue(data);
        });
    }
  }

  onSubmit(): void {
    const value = this.form.getRawValue();
    const payload: PhanPhoiChuongTrinhFormRequest = {
      week: Number(value[this.key.WEEK] ?? 0),
      classId: value[this.key.CLASS_ID],
      subjectId: value[this.key.SUBJECT_ID],
      subSubject: value[this.key.SUB_SUBJECT] ?? '',
      period: value[this.key.PERIOD] ?? '',
      lessonName: value[this.key.LESSON_NAME] ?? '',
      note: value[this.key.NOTE] ?? '',
    };

    if (this.data.type === TYPE_FORM.CREATE) {
      this.phanPhoiChuongTrinhService.create(payload).subscribe({
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

    this.phanPhoiChuongTrinhService.update(this.data.id!, payload).subscribe({
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
    this.title = 'Chỉnh sửa phân phối chương trình';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private loadOptions(): void {
    this.lopService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options = (
        data ?? []
      ).map((item: LopResponse) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.monHocService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options = (
        data ?? []
      ).map((item: MonHocOptionResponse) => ({
        value: item.id,
        label: item.name,
      }));
    });
  }
}
