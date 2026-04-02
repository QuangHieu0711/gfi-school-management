import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  KHOI_FORM,
  KHOI_KEY,
  KhoiFormRequest,
  KhoiResponse,
} from '@app/model/admin/khoi.model';
import { KhoiService } from '@app/service/admin/khoi.service';

@Component({
  selector: 'dialog-khoi',
  templateUrl: './dialog-khoi.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogKhoiComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = KHOI_FORM;
  key = KHOI_KEY;
  title = '';
  currentData: KhoiResponse | null = null;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogKhoiComponent>,
    private readonly khoiService: KhoiService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE; data?: KhoiResponse } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Ch\u1ec9nh s\u1eeda kh\u1ed1i';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi ti\u1ebft kh\u1ed1i';
        this.form.disable();
        break;
      default:
        this.title = 'Th\u00eam m\u1edbi kh\u1ed1i';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      this.getDetail(this.data.id);
    }
  }

  onSubmit() {
    const rawValue = this.form.getRawValue();
    const payload: KhoiFormRequest = {
      [KHOI_KEY.CODE]: rawValue[KHOI_KEY.CODE],
      [KHOI_KEY.NAME]: rawValue[KHOI_KEY.NAME],
      [KHOI_KEY.GRADE_NUMBER]: Number(rawValue[KHOI_KEY.GRADE_NUMBER]),
      [KHOI_KEY.STATUS]: Number(rawValue[KHOI_KEY.STATUS]),
      [KHOI_KEY.DESCRIPTION]: rawValue[KHOI_KEY.DESCRIPTION],
    };

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Ch\u1ec9nh s\u1eeda kh\u1ed1i';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private handleCreate(payload: KhoiFormRequest) {
    this.khoiService.create(payload).subscribe({
      next: () => {
        this.toastr.success('L\u01b0u th\u00e0nh c\u00f4ng', 'Th\u00e0nh c\u00f4ng');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'L\u01b0u th\u1ea5t b\u1ea1i',
          'Th\u1ea5t b\u1ea1i'
        );
      },
    });
  }

  private handleUpdate(payload: KhoiFormRequest) {
    this.khoiService.update(this.data.id!, payload).subscribe({
      next: () => {
        this.toastr.success(
          'C\u1eadp nh\u1eadt th\u00e0nh c\u00f4ng',
          'Th\u00e0nh c\u00f4ng'
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'C\u1eadp nh\u1eadt th\u1ea5t b\u1ea1i',
          'Th\u1ea5t b\u1ea1i'
        );
      },
    });
  }

  private getDetail(id: ID_TYPE) {
    this.khoiService.getById(id).subscribe(({ data }) => {
      this.currentData = data;
      this.form.patchValue(data);
    });
  }
}
