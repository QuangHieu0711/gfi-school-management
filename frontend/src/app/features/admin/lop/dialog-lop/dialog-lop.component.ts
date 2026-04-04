import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  LOP_FORM,
  LOP_KEY,
  LopFormRequest,
  LopResponse,
} from '@app/model/admin/lop.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { KhoiService } from '@app/service/admin/khoi.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';

@Component({
  selector: 'dialog-lop',
  templateUrl: './dialog-lop.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogLopComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = LOP_FORM;
  key = LOP_KEY;
  title = '';
  currentData: LopResponse | null = null;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogLopComponent>,
    private readonly lopService: LopService,
    private readonly donViService: DonViService,
    private readonly khoiService: KhoiService,
    private readonly namHocService: NamHocService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE; data?: LopResponse } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    this.loadSelectOptions();

    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa lớp';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết lớp';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới lớp';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      this.getDetail(this.data.id);
    }
  }

  onSubmit() {
    const rawValue = this.form.getRawValue();
    const payload: LopFormRequest = {
      [LOP_KEY.CODE]: rawValue[LOP_KEY.CODE],
      [LOP_KEY.NAME]: rawValue[LOP_KEY.NAME],
      [LOP_KEY.UNIT_ID]: rawValue[LOP_KEY.UNIT_ID],
      [LOP_KEY.GRADE_LEVEL_ID]: rawValue[LOP_KEY.GRADE_LEVEL_ID],
      [LOP_KEY.SCHOOL_YEAR_ID]: rawValue[LOP_KEY.SCHOOL_YEAR_ID],
      [LOP_KEY.STATUS]: Number(rawValue[LOP_KEY.STATUS]),
      [LOP_KEY.DESCRIPTION]: rawValue[LOP_KEY.DESCRIPTION],
    };

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Chỉnh sửa lớp';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private handleCreate(payload: LopFormRequest) {
    this.lopService.create(payload).subscribe({
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

  private handleUpdate(payload: LopFormRequest) {
    this.lopService.update(this.data.id!, payload).subscribe({
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

  private getDetail(id: ID_TYPE) {
    this.lopService.getById(id).subscribe(({ data }) => {
      this.currentData = data;
      this.form.patchValue(data);
    });
  }

  private loadSelectOptions() {
    this.donViService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.UNIT_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.khoiService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.GRADE_LEVEL_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.namHocService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.SCHOOL_YEAR_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });
  }
}
