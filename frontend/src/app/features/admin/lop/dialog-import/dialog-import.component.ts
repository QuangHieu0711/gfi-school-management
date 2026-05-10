/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { FILE_CONTROL, FormType } from '@model/form-control.model';
import { FileControlComponent } from '@components/form-group/file-control/file-control.component';
import { ComponentBaseAbstract } from '@layout';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { MATERIAL_MODULE } from '@modules';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { LopService } from '@app/service/admin/lop.service';

@Component({
  selector: 'dialog-import',
  standalone: true,
  templateUrl: './dialog-import.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FileControlComponent,
    ...MATERIAL_MODULE,
    AppDialogComponent,
    IconComponent,
  ],
})
export class DialogImportComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = [
    FILE_CONTROL({
      controlName: 'file',
      required: true,
      showLabel: false,
      label: 'Tệp tin',
    }),
  ];

  constructor(
    protected override injector: Injector,
    private readonly lopService: LopService,
    private dialogRef: MatDialogRef<DialogImportComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: any
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    super.componentInit();
  }

  downloadTemplate(): void {
    this.lopService.downloadTemplate().subscribe({
      next: (response: any) => {
        const blob = response?.data ?? response?.body ?? response;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'mau-import-lop-hoc.xlsx';
        a.click();
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
      },
      error: () => {
        this.toastr.error('Không tải được file mẫu', 'Thất bại');
      },
    });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const controlValue = this.form.get('file')?.value;

    const file: File | null =
      controlValue instanceof File ? controlValue : controlValue[0];
    if (!file) {
      this.toastr.warning('Chưa chọn file!', 'Cảnh báo');
      return;
    }

    const formData = new FormData();
    formData.append('file', file, file.name);

    this.lopService.import(formData).subscribe({
      next: ({ data, userMessage }) => {
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
                      : 'lop-hoc-import-error.xlsx'
                  );
                }
                this.dialogRef.close(true);
              }
            );
            return;
          }

          this.dialogRef.close(true);
          return;
        }

        this.toastr.success(message, 'Thành công');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Kết nạp dữ liệu thất bại',
          'Thất bại'
        );
      },
    });
  }

  private downloadErrorFile(
    errorFileToken: string,
    fallbackFileName?: string
  ): void {
    this.lopService.downloadImportErrorFile(errorFileToken).subscribe({
      next: ({ body, headers }) => {
        if (!body) {
          this.toastr.error('Không tải được file lỗi', 'Thất bại');
          return;
        }

        const fileName =
          this.getFileNameFromDisposition(headers.get('content-disposition')) ??
          fallbackFileName ??
          'lop-hoc-import-error.xlsx';

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
}
