/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FileControlComponent } from '@components/form-group/file-control/file-control.component';
import { ComponentBaseAbstract } from '@layout';
import { FILE_CONTROL, FormType } from '@model/form-control.model';
import { MATERIAL_MODULE } from '@modules';
import { AuthService } from '@service';
import { CanBoService } from '@app/service/admin/can-bo.service';

@Component({
  selector: 'dialog-import-can-bo',
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
export class DialogImportCanBoComponent extends ComponentBaseAbstract {
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
    private readonly canBoService: CanBoService,
    private readonly authService: AuthService,
    private readonly dialogRef: MatDialogRef<DialogImportCanBoComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  downloadTemplate(): void {
    const unitId = this.getUnitId();
    if (unitId == null) return;

    this.canBoService.downloadTemplate(unitId).subscribe({
      next: ({ body, headers }) => {
        if (!body) {
          this.toastr.error('Không tải được file mẫu', 'Thất bại');
          return;
        }

        const fileName =
          this.getFileNameFromDisposition(headers.get('content-disposition')) ??
          'template_import_danh_sach_can_bo.xlsx';

        this.fileService.downloadFile(body, fileName);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được file mẫu',
          'Thất bại'
        );
      },
    });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const unitId = this.getUnitId();
    if (unitId == null) return;

    const controlValue = this.form.get('file')?.value;
    const file: File | null =
      controlValue instanceof File ? controlValue : controlValue?.[0];

    if (!file) {
      this.toastr.warning('Chưa chọn file', 'Cảnh báo');
      return;
    }

    const formData = new FormData();
    formData.append('file', file, file.name);

    this.canBoService.import(unitId, formData).subscribe({
      next: ({ userMessage }) => {
        const message =
          typeof userMessage === 'string'
            ? userMessage
            : 'Nhập dữ liệu thành công';
        this.toastr.success(
          message,
          'Thành công'
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Nhập dữ liệu thất bại',
          'Thất bại'
        );
      },
    });
  }

  private getUnitId(): string | number | null {
    const unitId = this.authService.currentUser?.unit?.id ?? null;
    if (unitId == null || unitId === '') {
      this.toastr.warning(
        'Không xác định được đơn vị từ phiên đăng nhập',
        'Cảnh báo'
      );
      return null;
    }
    return unitId;
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
