import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  STAFF_FOREIGN_LANGUAGE_FORM,
  STAFF_FOREIGN_LANGUAGE_KEY,
  StaffForeignLanguageFormRequest,
  StaffForeignLanguageResponse,
} from '@app/model/admin/thong-tin-ngoai-ngu-can-bo.model';
import { StaffForeignLanguageService } from '@app/service/admin/thong-tin-ngoai-ngu-can-bo.service';

@Component({
  selector: 'dialog-thong-tin-ngoai-ngu',
  standalone: true,
  templateUrl: './dialog-thong-tin-ngoai-ngu.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogThongTinNgoaiNguComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly key = STAFF_FOREIGN_LANGUAGE_KEY;
  title = '';
  $formItem: FormType[] = structuredClone(STAFF_FOREIGN_LANGUAGE_FORM) as FormType[];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogThongTinNgoaiNguComponent>,
    private readonly staffForeignLanguageService: StaffForeignLanguageService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      staffId: ID_TYPE;
      id?: ID_TYPE;
      data?: StaffForeignLanguageResponse;
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa thông tin ngoại ngữ';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết thông tin ngoại ngữ';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới thông tin ngoại ngữ';
        break;
    }

    if (this.data.data) {
      this.patchData(this.data.data);
    } else if (
      this.data.id != null &&
      this.data.type !== this.TYPE_FORM.CREATE
    ) {
      this.staffForeignLanguageService
        .getById(this.data.id)
        .subscribe(({ data }) => {
          this.patchData(data);
        });
    }
  }

  onSubmit(): void {
    const payload = this.buildPayload();

    if (this.data.type === this.TYPE_FORM.CREATE) {
      this.staffForeignLanguageService.create(payload).subscribe({
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

    this.staffForeignLanguageService.update(this.data.id!, payload).subscribe({
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
    this.title = 'Chỉnh sửa thông tin ngoại ngữ';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private patchData(data: StaffForeignLanguageResponse): void {
    this.form.patchValue(
      {
        languageName: data.languageName ?? '',
        languageLevel: data.languageLevel ?? '',
        issueDate: data.issueDate?.slice(0, 10) ?? '',
        score: data.score ?? '',
        note: data.note ?? '',
      },
      { emitEvent: false }
    );
  }

  private buildPayload(): StaffForeignLanguageFormRequest {
    const value = this.form.getRawValue();

    return {
      staffId: this.data.staffId,
      languageName: value.languageName ?? '',
      languageLevel: value.languageLevel ?? '',
      issueDate: value.issueDate ?? '',
      score: value.score ?? '',
      note: value.note ?? '',
    };
  }
}
