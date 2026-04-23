/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FileControlComponent } from '@components/form-group/file-control/file-control.component';
import { ComponentBaseAbstract } from '@layout';
import {
  DATE_CONTROL,
  FILE_CONTROL,
  FormType,
  IOptions,
  SELECT_CONTROL,
} from '@model/form-control.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import { DiemDanhImportRequest } from '@app/model/admin/diem-danh.model';
import { DiemDanhService } from '@app/service/admin/diem-danh.service';

@Component({
  selector: 'dialog-import-diem-danh',
  standalone: true,
  templateUrl: './dialog-import.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FileControlComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
    AppDialogComponent,
    IconComponent,
  ],
})
export class DialogImportDiemDanhComponent extends ComponentBaseAbstract {
  readonly sessionTypeOptions: IOptions[] = [
    { value: 'SANG', label: 'Sáng' },
    { value: 'CHIEU', label: 'Chiều' },
  ];

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: 'sessionType',
      label: 'Buổi',
      placeholder: 'Chọn buổi',
      required: true,
      listOption: [],
      showLabel: true,
    }),
    DATE_CONTROL({
      controlName: 'month',
      label: 'Tháng',
      placeholder: 'Chọn tháng',
      required: true,
      showLabel: true,
      dateType: 'month',
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
    private readonly diemDanhService: DiemDanhService,
    private readonly dialogRef: MatDialogRef<DialogImportDiemDanhComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: DiemDanhImportRequest
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.findFormControl(this.$formItem, 'sessionType').options =
      this.sessionTypeOptions;
  }

  protected override componentInit(): void {
    this.form.patchValue(
      {
        sessionType: this.data.sessionType,
        month: this.toMonthInput(this.data.year, this.data.month),
      },
      { emitEvent: false }
    );
  }

  downloadTemplate(): void {
    const params = this.buildImportParams();
    if (!params) return;

    this.isDownloading = true;
    this.diemDanhService.downloadTemplate(params).subscribe({
      next: ({ body, headers }) => {
        this.isDownloading = false;

        if (!body) {
          this.toastr.error('Không tải được file mẫu', 'Thất bại');
          return;
        }

        const fileName =
          this.getFileNameFromDisposition(headers.get('content-disposition')) ??
          'template_import_diem_danh.xlsx';

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
    if (this.form.invalid || this.isSubmitting) return;

    const params = this.buildImportParams();
    if (!params) return;

    const file = this.getSelectedFile();
    if (!file) {
      this.toastr.warning('Chưa chọn file import', 'Cảnh báo');
      return;
    }

    this.isSubmitting = true;
    this.diemDanhService.importExcel(params, file).subscribe({
      next: ({ data, userMessage }) => {
        this.isSubmitting = false;
        const successCount = data?.successCount ?? 0;
        const failedCount = data?.failedCount ?? 0;
        const message =
          typeof userMessage === 'string'
            ? userMessage
            : `Import hoàn tất: ${successCount} bản ghi thành công, ${failedCount} bản ghi lỗi`;

        if (failedCount > 0) {
          this.toastr.warning(message, 'Kết nạp hoàn tất');

          if (data?.hasErrorFile && data.errorFileToken) {
            this.dialog.confirm(
              {
                title: 'Tải file lỗi',
                message:
                  'Có bản ghi import lỗi. Bạn có muốn tải file lỗi để chỉnh sửa không?',
              },
              (confirmed?: boolean) => {
                if (confirmed) {
                  this.downloadErrorFile(
                    String(data.errorFileToken),
                    typeof data.errorFileName === 'string'
                      ? data.errorFileName
                      : undefined
                  );
                }
                this.dialogRef.close({
                  refresh: true,
                  month: this.getSelectedMonthValue(),
                  sessionType: params.sessionType,
                });
              }
            );
            return;
          }

          this.dialogRef.close({
            refresh: true,
            month: this.getSelectedMonthValue(),
            sessionType: params.sessionType,
          });
          return;
        }

        this.toastr.success(message, 'Thành công');
        this.dialogRef.close({
          refresh: true,
          month: this.getSelectedMonthValue(),
          sessionType: params.sessionType,
        });
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

  private buildImportParams(): DiemDanhImportRequest | null {
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    const monthValue = this.getSelectedMonthValue();
    const [yearStr, monthStr] = monthValue.split('-');
    const year = Number(yearStr);
    const month = Number(monthStr);

    if (!this.data.classroomId || !sessionType || !year || !month) {
      this.toastr.warning('Chưa chọn đủ buổi hoặc tháng', 'Cảnh báo');
      return null;
    }

    return {
      classroomId: this.data.classroomId,
      year,
      month,
      sessionType,
    };
  }

  private getSelectedFile(): File | null {
    const controlValue = this.form.get('file')?.value;
    const file: File | null =
      controlValue instanceof File
        ? controlValue
        : controlValue?.[0]?.file ??
          controlValue?.[0]?.rawFile ??
          controlValue?.[0] ??
          null;

    return file instanceof File ? file : null;
  }

  private getSelectedMonthValue(): string {
    const rawValue = this.form.get('month')?.value;
    return this.normalizeMonthValue(rawValue);
  }

  private normalizeMonthValue(value: unknown): string {
    if (value instanceof Date && !Number.isNaN(value.getTime())) {
      return this.toMonthInput(value.getFullYear(), value.getMonth() + 1);
    }

    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (/^\d{4}-\d{2}$/.test(trimmed)) return trimmed;
      if (/^\d{4}-\d{2}-\d{2}/.test(trimmed)) return trimmed.slice(0, 7);

      const parsedDate = new Date(trimmed);
      if (!Number.isNaN(parsedDate.getTime())) {
        return this.toMonthInput(
          parsedDate.getFullYear(),
          parsedDate.getMonth() + 1
        );
      }
    }

    return '';
  }

  private downloadErrorFile(
    errorFileToken: string,
    fallbackFileName?: string
  ): void {
    this.diemDanhService.downloadImportErrorFile(errorFileToken).subscribe({
      next: ({ body, headers }) => {
        if (!body) {
          this.toastr.error('Không tải được file lỗi', 'Thất bại');
          return;
        }

        const fileName =
          this.getFileNameFromDisposition(headers.get('content-disposition')) ??
          fallbackFileName ??
          'diem-danh-import-error.xlsx';

        this.fileService.downloadFile(body, fileName);
        this.toastr.success('Tải file lỗi thành công', 'Thành công');
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được file lỗi',
          'Thất bại'
        );
      },
    });
  }

  private getFileNameFromDisposition(
    disposition: string | null
  ): string | null {
    if (!disposition) return null;

    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    if (utf8Match) {
      return decodeURIComponent(utf8Match);
    }

    return disposition.match(/filename="?([^"]+)"?/i)?.[1] ?? null;
  }

  private toMonthInput(year: number, month: number): string {
    return `${year}-${`${month}`.padStart(2, '0')}`;
  }
}
