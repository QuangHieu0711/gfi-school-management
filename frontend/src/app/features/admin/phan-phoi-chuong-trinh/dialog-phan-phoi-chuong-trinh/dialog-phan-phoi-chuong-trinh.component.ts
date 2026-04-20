import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { filter, take } from 'rxjs';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { AuthService } from '@service';

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
import { WeekConfigService } from '@app/service/admin/week-config.service';

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
  private orderNumber = 0;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogPhanPhoiChuongTrinhComponent>,
    private readonly phanPhoiChuongTrinhService: PhanPhoiChuongTrinhService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    private readonly weekConfigService: WeekConfigService,
    private readonly authService: AuthService,
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
      this.patchFormFromResponse(this.data.data);
      this.ensureClassOption(this.data.data);
    } else if (
      this.data.type !== this.TYPE_FORM.CREATE &&
      this.data.id != null
    ) {
      this.phanPhoiChuongTrinhService
        .getById(this.data.id)
        .subscribe(({ data }) => {
          this.patchFormFromResponse(data);
          this.ensureClassOption(data);
        });
    } else {
      this.loadClassOptionsForCurrentUnit();
    }
  }

  onSubmit(): void {
    const unitId = this.getCurrentUnitId();
    if (unitId == null) {
      this.toastr.warning(
        'Không xác định được đơn vị từ tài khoản đăng nhập',
        'Cảnh báo'
      );
      return;
    }

    const value = this.form.getRawValue();
    const payload: PhanPhoiChuongTrinhFormRequest = {
      unitId,
      week: Number(value[this.key.WEEK] ?? 0),
      weekNumber: Number(value[this.key.WEEK] ?? 0),
      classId: value[this.key.CLASS_ID],
      classroomId: value[this.key.CLASS_ID],
      subjectId: value[this.key.SUBJECT_ID],
      period: value[this.key.PERIOD] ?? '',
      periodPpct: value[this.key.PERIOD] ?? '',
      orderNumber: this.orderNumber,
      lessonName: value[this.key.LESSON_NAME] ?? '',
      note: value[this.key.NOTE] ?? '',
    };

    this.phanPhoiChuongTrinhService.create(payload).subscribe({
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

  switchUpdate(): void {
    this.form.enable();
    this.title = 'Chỉnh sửa phân phối chương trình';
    this.data.type = this.TYPE_FORM.UPDATE;
    this.loadClassOptionsForCurrentUnit();
  }

  private loadOptions(): void {
    this.weekConfigService.getComboboxOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.WEEK).options = (
        data ?? []
      ).map((item) => ({
        value: this.resolveWeekValue(item.id, item.name),
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

  private loadClassOptions(unitId: ID_TYPE): void {
    this.lopService.getOptions({ unitId }).subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options = (
        data ?? []
      ).map((item: LopResponse) => ({
        value: item.id,
        label: item.name,
      }));
    });
  }

  private loadClassOptionsForCurrentUnit(): void {
    const unitId = this.getCurrentUnitId();
    if (unitId != null) {
      this.loadClassOptions(unitId);
      return;
    }

    this.authService.currentUser$
      .pipe(
        filter((user) => !!user?.unit?.id),
        take(1)
      )
      .subscribe((user) => {
        const updatedUnitId = user?.unit?.id;
        if (updatedUnitId != null) {
          this.loadClassOptions(updatedUnitId);
        }
      });
  }

  private ensureClassOption(data: PhanPhoiChuongTrinhResponse): void {
    const rawData = data as Record<string, unknown>;
    const classIdValue =
      data[this.key.CLASS_ID] ??
      (rawData[this.key.CLASSROOM_ID] as ID_TYPE | undefined) ??
      undefined;
    const classLabel =
      data[this.key.CLASS_NAME] ??
      (rawData['classroomName'] as string | undefined) ??
      '';

    if (classIdValue != null && classLabel) {
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options = [
        {
          value: classIdValue,
          label: classLabel,
        },
      ];
      return;
    }

    this.loadClassOptionsForCurrentUnit();
  }

  private patchFormFromResponse(data: PhanPhoiChuongTrinhResponse): void {
    const rawData = data as Record<string, unknown>;
    const weekValue = Number(rawData['weekNumber'] ?? data[this.key.WEEK] ?? 0);
    const classIdValue =
      data[this.key.CLASS_ID] ??
      (rawData[this.key.CLASSROOM_ID] as ID_TYPE | undefined) ??
      undefined;
    const subjectIdValue =
      data[this.key.SUBJECT_ID] ??
      (rawData[this.key.SUBJECT_ID] as ID_TYPE | undefined) ??
      undefined;

    this.orderNumber = Number(
      data[this.key.ORDER_NUMBER] ?? rawData['orderNumber'] ?? 0
    );

    this.form.patchValue({
      ...data,
      [this.key.WEEK]: weekValue,
      [this.key.CLASS_ID]: classIdValue,
      [this.key.SUBJECT_ID]: subjectIdValue,
      [this.key.PERIOD]:
        data[this.key.PERIOD] ??
        (rawData['periodPpct'] as string | undefined) ??
        '',
    });
  }

  private resolveWeekValue(id: ID_TYPE, name: string): ID_TYPE | number {
    const parsed = Number(String(name ?? '').replace(/[^0-9]/g, ''));
    return Number.isFinite(parsed) && parsed > 0 ? parsed : id;
  }

  private getCurrentUnitId(): ID_TYPE | null {
    return this.authService.currentUser?.unit?.id ?? null;
  }
}
