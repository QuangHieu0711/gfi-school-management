import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType, IOptions, SELECT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  PHAN_CONG_GIANG_DAY_FORM,
  PHAN_CONG_GIANG_DAY_KEY,
  PhanCongGiangDayFormRequest,
  PhanCongGiangDayResponse,
} from '@app/model/admin/phan-cong-giang-day.model';
import { CanBoResponse } from '@app/model/admin/can-bo.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { MonHocResponse } from '@app/model/admin/mon-hoc.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { CanBoService } from '@app/service/admin/can-bo.service';
import { DonViService } from '@app/service/admin/don-vi.service';
import { LopService } from '@app/service/admin/lop.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';

@Component({
  selector: 'dialog-phan-cong-giang-day',
  standalone: true,
  templateUrl: './dialog-phan-cong-giang-day.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogPhanCongGiangDayComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly key = PHAN_CONG_GIANG_DAY_KEY;
  title = '';
  $formItem: FormType[] = structuredClone(PHAN_CONG_GIANG_DAY_FORM) as FormType[];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogPhanCongGiangDayComponent>,
    private readonly phanCongService: PhanCongGiangDayService,
    private readonly namHocService: NamHocService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    private readonly donViService: DonViService,
    private readonly canBoService: CanBoService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      staffId?: ID_TYPE;
      id?: ID_TYPE;
      data?: PhanCongGiangDayResponse;
    }
  ) {
    super(injector);
    if (!data.staffId) {
      this.$formItem.unshift(
        SELECT_CONTROL({
          controlName: this.key.STAFF_ID,
          label: 'Can bo',
          placeholder: 'Chon can bo',
          required: true,
          listOption: [],
        }) as FormType
      );
    }
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chinh sua phan cong giang day';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiet phan cong giang day';
        this.form.disable();
        break;
      default:
        this.title = 'Them moi phan cong giang day';
        break;
    }

    this.loadOptions();

    if (this.data.data) {
      this.patchData(this.data.data);
    } else if (
      this.data.id != null &&
      this.data.type !== this.TYPE_FORM.CREATE
    ) {
      this.phanCongService.getById(this.data.id).subscribe(({ data }) => {
        this.patchData(data);
      });
    }
  }

  onSubmit(): void {
    const payload = this.buildPayload();

    if (this.data.type === this.TYPE_FORM.CREATE) {
      this.phanCongService.create(payload).subscribe({
        next: () => {
          this.toastr.success('Luu thanh cong', 'Thanh cong');
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ?? error?.error?.message ?? 'Luu that bai',
            'That bai'
          );
        },
      });
      return;
    }

    this.phanCongService.update(this.data.id!, payload).subscribe({
      next: () => {
        this.toastr.success('Cap nhat thanh cong', 'Thanh cong');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Cap nhat that bai',
          'That bai'
        );
      },
    });
  }

  switchUpdate(): void {
    this.form.enable();
    this.title = 'Chinh sua phan cong giang day';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private loadOptions(): void {
    this.namHocService.getOptions().subscribe((res) => {
      this.findFormControl(this.$formItem, this.key.SCHOOL_YEAR_ID).options =
        (res.data ?? []).map((item: NamHocOptionResponse) => ({
          label: item.name,
          value: item.id,
        })) as IOptions[];
    });

    this.lopService.getOptions().subscribe((res) => {
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options = (
        res.data ?? []
      ).map((item: LopResponse) => ({
        label: item.name,
        value: item.id,
      })) as IOptions[];
    });

    this.monHocService.filter({ pageNow: 1, pageSize: 1000 }).subscribe((res) => {
      const items = res.data?.items ?? res.data?.data ?? [];
      this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options = items.map(
        (item: MonHocResponse) => ({
          label: item.name,
          value: item.id,
        })
      ) as IOptions[];
    });

    this.donViService.getOptions().subscribe((res) => {
      this.findFormControl(this.$formItem, this.key.DEPARTMENT_ID).options = (
        res.data ?? []
      ).map((item) => ({
        label: item.name,
        value: item.id,
      })) as IOptions[];
    });

    if (!this.data.staffId) {
      this.canBoService.filter({ pageNow: 1, pageSize: 1000 }).subscribe((res) => {
        const items = res.data?.items ?? res.data?.data ?? [];
        this.findFormControl(this.$formItem, this.key.STAFF_ID).options = items.map(
          (item: CanBoResponse) => ({
            label: `${item.staffCode ?? ''} - ${item.fullName ?? ''}`.trim(),
            value: item.id,
          })
        ) as IOptions[];
      });
    }
  }

  private patchData(data: PhanCongGiangDayResponse): void {
    this.form.patchValue(
      {
        schoolYearId: data.schoolYearId ?? '',
        classId: data.classId ?? '',
        subjectId: data.subjectId ?? '',
        departmentId: data.departmentId ?? '',
        staffId: data.staffId ?? '',
        isHomeroom: data.isHomeroom ?? false,
        teachingLoad: data.teachingLoad ?? '',
        note: data.note ?? '',
      },
      { emitEvent: false }
    );
  }

  private buildPayload(): PhanCongGiangDayFormRequest {
    const value = this.form.getRawValue();

    return {
      staffId: value.staffId ?? this.data.staffId,
      schoolYearId: value.schoolYearId ?? '',
      classId: value.classId ?? '',
      subjectId: value.subjectId ?? '',
      departmentId: value.departmentId ?? '',
      isHomeroom: value.isHomeroom ?? false,
      teachingLoad:
        value.teachingLoad === '' || value.teachingLoad == null
          ? undefined
          : Number(value.teachingLoad),
      note: value.note ?? '',
    };
  }
}
