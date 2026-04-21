import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FILE_CONTROL, FormType, SELECT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { AuthService } from '@service';

import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import {
  PHAN_CONG_GIANG_DAY_KEY,
  PhanCongGiangDayImportRequest,
} from '@app/model/admin/phan-cong-giang-day.model';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';

@Component({
  selector: 'dialog-import-phan-cong-giang-day',
  templateUrl: './dialog-import.component.html',
  imports: [
    CommonModule,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
    AppDialogComponent,
    IconComponent,
  ],
})
export class DialogImportPhanCongGiangDayComponent extends ComponentBaseAbstract {
  readonly key = PHAN_CONG_GIANG_DAY_KEY;

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: this.key.SCHOOL_YEAR_ID,
      label: 'Năm học',
      placeholder: 'Chọn năm học',
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
    private readonly dialogRef: MatDialogRef<DialogImportPhanCongGiangDayComponent>,
    private readonly phanCongGiangDayService: PhanCongGiangDayService,
    private readonly namHocService: NamHocService,
    private readonly authService: AuthService,
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
  }

  downloadTemplate(): void {
    if (!this.validateMetadataForm()) return;

    this.isDownloading = true;
    this.phanCongGiangDayService
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
            'template_phan_cong_giang_day.xlsx'
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
    this.phanCongGiangDayService
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
        this.findFormControl(this.$formItem, this.key.SCHOOL_YEAR_ID).options = (
          data ?? []
        ).map((item: NamHocOptionResponse) => ({
          value: item.id,
          label: item.name,
        }));

        this.namHocService.getCurrent().subscribe({
          next: ({ data: currentSchoolYear }) => {
            if (currentSchoolYear?.id != null) {
              this.form
                .get(this.key.SCHOOL_YEAR_ID)
                ?.setValue(currentSchoolYear.id, { emitEvent: false });
            }
          },
        });
      },
    });
  }

  private validateMetadataForm(): boolean {
    this.form.get(this.key.SCHOOL_YEAR_ID)?.markAsTouched();
    return !!this.form.get(this.key.SCHOOL_YEAR_ID)?.value;
  }

  private buildImportParams(): PhanCongGiangDayImportRequest {
    const value = this.form.getRawValue();
    return {
      schoolYearId: value[this.key.SCHOOL_YEAR_ID],
      unitId: this.authService.currentUser?.unit?.id as ID_TYPE,
    };
  }

  private getSelectedFile(): File | null {
    const rawFile = this.form.get('file')?.value;
    if (rawFile instanceof File) return rawFile;
    if (Array.isArray(rawFile) && rawFile.length > 0) {
      const item = rawFile[0];
      return item instanceof File ? item : (item?.file ?? item?.rawFile ?? null);
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

    return disposition.match(/filename=\"?([^\"]+)\"?/i)?.[1] || fallbackName;
  }
}
