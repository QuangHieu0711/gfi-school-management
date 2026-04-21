/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Inject, Injector } from '@angular/core';
import { FILE_CONTROL, FormType } from '@model/form-control.model';
import { FileControlComponent } from '@components/form-group/file-control/file-control.component';
import { ComponentBaseAbstract } from '@layout';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { MATERIAL_MODULE } from '@modules';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DonViService } from '@app/service/admin/don-vi.service';

@Component({
  selector: 'dialog-import',
  templateUrl: './dialog-import.component.html',
  imports: [
    FileControlComponent,
    AppDialogComponent,
    ...MATERIAL_MODULE,
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
    private readonly donViService: DonViService,
    private dialogRef: MatDialogRef<DialogImportComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: any
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {}

  downloadTemplate(): void {
    this.donViService.downloadTemplate().subscribe({
      next: (response: any) => {
        const blob = response?.data ?? response;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'template_import_danh_sach_don_vi.xlsx';
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

    this.donViService.import(formData).subscribe({
      next: () => {
        this.toastr.success('Tải lên file thành công', 'Thành công');
        this.dialogRef.close(true);
      },
    });
  }
}
