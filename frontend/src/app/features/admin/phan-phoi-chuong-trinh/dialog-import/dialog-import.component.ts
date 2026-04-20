import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { distinctUntilChanged, takeUntil } from 'rxjs';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import {
  FILE_CONTROL,
  FormType,
  SELECT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import { HocKyResponse, HOC_KY_KEY } from '@app/model/admin/hoc-ky.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { MonHocOptionResponse } from '@app/model/admin/mon-hoc.model';
import {
  PHAN_PHOI_CHUONG_TRINH_KEY,
  PhanPhoiChuongTrinhImportRequest,
} from '@app/model/admin/phan-phoi-chuong-trinh.model';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { LopService } from '@app/service/admin/lop.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { PhanPhoiChuongTrinhService } from '@app/service/admin/phan-phoi-chuong-trinh.service';

@Component({
  selector: 'dialog-import',
  templateUrl: './dialog-import.component.html',
  imports: [
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
    AppDialogComponent,
    IconComponent,
  ],
})
export class DialogImportComponent extends ComponentBaseAbstract {
  readonly key = PHAN_PHOI_CHUONG_TRINH_KEY;

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: this.key.SCHOOL_YEAR_ID,
      label: 'Năm học',
      placeholder: 'Chọn năm học',
      required: true,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: this.key.SEMESTER_ID,
      label: 'Học kỳ',
      placeholder: 'Chọn học kỳ',
      required: true,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: this.key.CLASSROOM_ID,
      label: 'Lớp',
      placeholder: 'Chọn lớp',
      required: true,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: this.key.SUBJECT_ID,
      label: 'Môn học',
      placeholder: 'Chọn môn học',
      required: true,
      clearable: true,
      listOption: [],
    }),
    FILE_CONTROL({
      controlName: 'file',
      required: true,
      showLabel: false,
      label: 'Tệp tin',
    }),
  ];

  isSubmitting = false;
  isDownloading = false;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogImportComponent>,
    private readonly phanPhoiChuongTrinhService: PhanPhoiChuongTrinhService,
    private readonly namHocService: NamHocService,
    private readonly hocKyService: HocKyService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      id?: ID_TYPE;
      data?: unknown;
    } = { type: TYPE_FORM.CREATE }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    this.loadSchoolYearOptions();
    this.loadSubjectOptions();
    this.watchSchoolYearChanges();
  }

  downloadTemplate(): void {
    if (!this.validateMetadataForm()) return;

    this.isDownloading = true;
    this.phanPhoiChuongTrinhService
      .downloadTemplate(this.buildImportParams())
      .subscribe({
        next: ({ body, headers }) => {
          this.isDownloading = false;

          if (!body) {
            this.toastr.error('Không tải được file mẫu', 'Thất bại');
            return;
          }

          const fileName = this.getFileNameFromDisposition(
            headers.get('content-disposition'),
            'template_phan_phoi_chuong_trinh.xlsx'
          );

          this.fileService.downloadFile(body, fileName);
          this.toastr.success('Tải mẫu thành công', 'Thành công');
        },
        error: (error) => {
          this.isDownloading = false;
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Tải file mẫu thất bại',
            'Thất bại'
          );
        },
      });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const file = this.getSelectedFile();
    if (!file) {
      this.toastr.warning('Chưa chọn file import', 'Cảnh báo');
      return;
    }

    this.isSubmitting = true;
    this.phanPhoiChuongTrinhService
      .importExcel(this.buildImportParams(), file)
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.toastr.success('Kết nạp dữ liệu thành công', 'Thành công');
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.isSubmitting = false;
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Kết nạp dữ liệu thất bại',
            'Thất bại'
          );
        },
      });
  }

  private loadSchoolYearOptions(): void {
    this.namHocService.getOptions().subscribe({
      next: ({ data }) => {
        this.updateOptions(this.key.SCHOOL_YEAR_ID, data, 'id', 'name');
      },
    });
  }

  private loadSemesterOptions(schoolYearId: ID_TYPE): void {
    this.hocKyService.filter({ schoolYearId }).subscribe({
      next: ({ data }) => {
        const items = Array.isArray(data) ? data : data?.items || [];
        this.updateOptions(
          this.key.SEMESTER_ID,
          items,
          HOC_KY_KEY.ID,
          HOC_KY_KEY.NAME
        );
      },
      error: () => {
        this.updateOptions(this.key.SEMESTER_ID, []);
      },
    });
  }

  private loadClassroomOptions(schoolYearId: ID_TYPE): void {
    this.lopService.getOptions({ schoolYearId }).subscribe({
      next: ({ data }) => {
        this.updateOptions(this.key.CLASSROOM_ID, data, 'id', 'name');
      },
      error: () => {
        this.updateOptions(this.key.CLASSROOM_ID, []);
      },
    });
  }

  private loadSubjectOptions(): void {
    this.monHocService.getOptions().subscribe({
      next: ({ data }) => {
        this.updateOptions(this.key.SUBJECT_ID, data, 'id', 'name');
      },
    });
  }

  private updateOptions(
    controlName: string,
    items: any[] = [],
    valueKey = 'id',
    labelKey = 'name'
  ): void {
    this.findFormControl(this.$formItem, controlName).options = (
      items ?? []
    ).map((item) => ({
      value: item[valueKey],
      label: item[labelKey] ?? '',
    }));
  }

  private watchSchoolYearChanges(): void {
    this.form
      .get(this.key.SCHOOL_YEAR_ID)
      ?.valueChanges.pipe(distinctUntilChanged(), takeUntil(this.ngUnsubscribe))
      .subscribe((schoolYearId) => {
        this.form.patchValue(
          {
            [this.key.SEMESTER_ID]: null,
            [this.key.CLASSROOM_ID]: null,
          },
          { emitEvent: false }
        );

        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];
        this.findFormControl(this.$formItem, this.key.CLASSROOM_ID).options =
          [];

        if (schoolYearId == null || schoolYearId === '') {
          return;
        }

        this.loadSemesterOptions(schoolYearId as ID_TYPE);
        this.loadClassroomOptions(schoolYearId as ID_TYPE);
      });
  }

  private validateMetadataForm(): boolean {
    const keys = [
      this.key.SCHOOL_YEAR_ID,
      this.key.SEMESTER_ID,
      this.key.CLASSROOM_ID,
      this.key.SUBJECT_ID,
    ];

    keys.forEach((key) => this.form.get(key)?.markAsTouched());

    const values = this.form.getRawValue();
    const isValid = keys.every(
      (key) => values[key] != null && values[key] !== ''
    );

    if (!isValid) {
      this.toastr.warning(
        'Vui lòng chọn đủ năm học, học kỳ, lớp và môn học',
        'Cảnh báo'
      );
    }

    return isValid;
  }

  private buildImportParams(): PhanPhoiChuongTrinhImportRequest {
    const values = this.form.getRawValue();

    return {
      schoolYearId: values[this.key.SCHOOL_YEAR_ID],
      semesterId: values[this.key.SEMESTER_ID],
      classroomId: values[this.key.CLASSROOM_ID],
      subjectId: values[this.key.SUBJECT_ID],
    };
  }

  private getSelectedFile(): File | null {
    const controlValue = this.form.get('file')?.value;

    if (controlValue instanceof File) {
      return controlValue;
    }

    if (Array.isArray(controlValue) && controlValue.length > 0) {
      const item = controlValue[0];
      return item instanceof File
        ? item
        : (item?.file ?? item?.rawFile ?? null);
    }

    return null;
  }

  private getFileNameFromDisposition(
    disposition: string | null,
    fallbackName: string
  ): string {
    if (!disposition) return fallbackName;

    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    if (utf8Match) {
      return decodeURIComponent(utf8Match);
    }

    return disposition.match(/filename="?([^"]+)"?/i)?.[1] || fallbackName;
  }
}
