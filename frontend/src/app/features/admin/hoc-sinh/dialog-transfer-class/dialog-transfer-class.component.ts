import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { ComponentBaseAbstract } from '@layout';
import {
  CHECKBOX_CONTROL,
  DATE_CONTROL,
  FormType,
  IOptions,
  SELECT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import {
  HocSinhResponse,
  HocSinhTransferClassRequest,
} from '@app/model/admin/hoc-sinh.model';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';

const TRANSFER_FORM_KEY = {
  TARGET_SCHOOL_YEAR_ID: 'targetSchoolYearId',
  TARGET_CLASS_ID: 'targetClassId',
  ENROLLED_AT: 'enrolledAt',
  IS_REPEATER: 'isRepeater',
} as const;

type TransferFormKey =
  (typeof TRANSFER_FORM_KEY)[keyof typeof TRANSFER_FORM_KEY];

@Component({
  selector: 'dialog-transfer-class',
  standalone: true,
  templateUrl: './dialog-transfer-class.component.html',
  styleUrls: ['./dialog-transfer-class.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppDialogComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class DialogTransferClassComponent extends ComponentBaseAbstract {
  readonly title = 'Chuyển lớp học sinh';
  readonly formKey = TRANSFER_FORM_KEY;
  readonly $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: 'targetSchoolYearId',
      label: 'Năm học đích',
      placeholder: 'Chọn năm học đích',
      required: true,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: 'targetClassId',
      label: 'Lớp đích',
      placeholder: 'Chọn lớp đích',
      required: true,
      clearable: true,
      listOption: [],
      disabled: true,
    }),
    DATE_CONTROL({
      controlName: 'enrolledAt',
      label: 'Ngày chuyển lớp',
      placeholder: 'Chọn ngày chuyển lớp',
      required: false,
    }),
    CHECKBOX_CONTROL({
      controlName: 'isRepeater',
      label: 'Lưu ban',
      required: false,
      defaultValue: false,
    }),
  ];

  schoolYearOptions: IOptions[] = [];
  classOptions: IOptions[] = [];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogTransferClassComponent>,
    private readonly hocSinhService: HocSinhService,
    private readonly namHocService: NamHocService,
    private readonly lopService: LopService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      unitId: ID_TYPE;
      students: HocSinhResponse[];
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  get selectedStudents(): HocSinhResponse[] {
    return this.data?.students ?? [];
  }

  protected override componentInit(): void {
    this.loadSchoolYearOptions();
    this.form
      .get(this.formKey.TARGET_SCHOOL_YEAR_ID)
      ?.valueChanges.subscribe((schoolYearId) => {
        this.onTargetSchoolYearChanged(schoolYearId);
      });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const rawValue = this.form.getRawValue();
    const payload: HocSinhTransferClassRequest = {
      studentIds: this.selectedStudents.map((student) => student.id),
      targetSchoolYearId: rawValue[this.formKey.TARGET_SCHOOL_YEAR_ID],
      targetClassId: rawValue[this.formKey.TARGET_CLASS_ID],
      enrolledAt: this.normalizeDateValue(rawValue[this.formKey.ENROLLED_AT]),
      isRepeater: !!rawValue[this.formKey.IS_REPEATER],
    };

    this.hocSinhService.transferClass(payload).subscribe({
      next: ({ data }) => {
        const count = data?.transferredCount ?? this.selectedStudents.length;
        this.toastr.success(`Đã chuyển lớp ${count} học sinh`, 'Thành công');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Chuyển lớp thất bại',
          'Thất bại'
        );
      },
    });
  }

  private loadSchoolYearOptions(): void {
    this.namHocService.getOptions().subscribe({
      next: ({ data }) => {
        this.schoolYearOptions = (data ?? []).map((item) => ({
          value: item.id,
          label: item.name,
        }));
        this.findFormControl(
          this.$formItem,
          this.formKey.TARGET_SCHOOL_YEAR_ID
        ).options = this.schoolYearOptions;

        this.namHocService.getCurrent().subscribe({
          next: ({ data: current }) => {
            if (!current?.id) return;
            this.form
              .get(this.formKey.TARGET_SCHOOL_YEAR_ID)
              ?.setValue(current.id, { emitEvent: true });
          },
        });
      },
      error: () => {
        this.schoolYearOptions = [];
      },
    });
  }

  private onTargetSchoolYearChanged(schoolYearId: ID_TYPE | null): void {
    const classControl = this.form.get(this.formKey.TARGET_CLASS_ID);
    classControl?.reset('', { emitEvent: false });
    classControl?.disable({ emitEvent: false });
    this.classOptions = [];
    this.findFormControl(this.$formItem, this.formKey.TARGET_CLASS_ID).options =
      [];

    if (schoolYearId == null || schoolYearId === '') {
      return;
    }

    this.lopService
      .getOptions({
        unitId: this.data.unitId,
        schoolYearId,
      })
      .subscribe({
        next: ({ data }) => {
          this.classOptions = (data ?? []).map((item) => ({
            value: item.id,
            label: item.name,
          }));
          this.findFormControl(
            this.$formItem,
            this.formKey.TARGET_CLASS_ID
          ).options = this.classOptions;
          classControl?.enable({ emitEvent: false });
        },
        error: () => {
          this.classOptions = [];
        },
      });
  }

  private normalizeDateValue(value: unknown): string | undefined {
    if (typeof value !== 'string' || !value) return undefined;
    if (value.includes('/')) {
      const [day, month, year] = value.split('/');
      if (day && month && year) {
        return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
      }
    }
    return value.slice(0, 10);
  }
}
