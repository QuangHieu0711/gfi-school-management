/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { SELECT_CONTROL, FILE_CONTROL, FormType } from '@model/form-control.model';
import { FileControlComponent } from '@components/form-group/file-control/file-control.component';
import { SelectControlComponent } from '@components/form-group/select-control/select-control.component';

import { ComponentBaseAbstract } from '@layout';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { MATERIAL_MODULE } from '@modules';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EvaluationService } from '@app/service/admin/evaluation.service';

@Component({
  selector: 'dialog-import-evaluation',
  standalone: true,
  templateUrl: './dialog-import-evaluation.component.html',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FileControlComponent,
    SelectControlComponent,
    ...MATERIAL_MODULE,
    AppDialogComponent,
    IconComponent,
  ],
})
export class DialogImportEvaluationComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: 'classroomId',
      label: 'Lớp học',
      required: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: 'subjectId',
      label: 'Môn học',
      required: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: 'semesterId',
      label: 'Học kỳ',
      required: true,
      listOption: [],
    }),

    FILE_CONTROL({
      controlName: 'file',
      required: true,
      showLabel: true,
      label: 'Tệp tin',
    }),
  ];


  constructor(
    protected override injector: Injector,
    private readonly evaluationService: EvaluationService,
    private dialogRef: MatDialogRef<DialogImportEvaluationComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      classroomId: number;
      subjectId: number;
      semesterId: number;
      classroomName: string;
      subjectName: string;
      semesterName: string;
      classroomOptions: any[];
      semesterOptions: any[];
      subjectsByClassId: Map<string, any[]>;
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
    
    // Initialize options
    this.findFormControl(this.$formItem, 'classroomId').options = this.data.classroomOptions;
    this.findFormControl(this.$formItem, 'semesterId').options = this.data.semesterOptions;
    
    this.form.patchValue({
      classroomId: this.data.classroomId,
      semesterId: this.data.semesterId,
      subjectId: this.data.subjectId
    });

    this.updateSubjectOptions(this.data.classroomId);

    // Watch for classroom changes
    this.form.get('classroomId')?.valueChanges.subscribe(val => {
      this.updateSubjectOptions(val);
      this.form.get('subjectId')?.setValue(null);
    });
  }

  private updateSubjectOptions(classroomId: any): void {
    const subjects = this.data.subjectsByClassId.get(`${classroomId}`) || [];
    const subjectOptions = subjects.map(s => ({
      label: s.subjectName,
      value: s.subjectId
    }));
    this.findFormControl(this.$formItem, 'subjectId').options = subjectOptions;
  }


  downloadTemplate(): void {
    const { classroomId, subjectId, semesterId } = this.form.getRawValue();
    if (!classroomId || !subjectId || !semesterId) {
      this.toastr.warning('Vui lòng chọn đầy đủ Lớp, Môn và Học kỳ để tải mẫu', 'Cảnh báo');
      return;
    }

    this.evaluationService.exportTemplate(classroomId, subjectId, semesterId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        
        const classOption = this.findFormControl(this.$formItem, 'classroomId').options.find(o => o.value === classroomId);
        const subjectOption = this.findFormControl(this.$formItem, 'subjectId').options.find(o => o.value === subjectId);
        
        a.download = `mau_import_danh_gia_${classOption?.label}_${subjectOption?.label}.xlsx`;
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

    const { classroomId, subjectId, semesterId } = this.form.getRawValue();
    const controlValue = this.form.get('file')?.value;
    const file: File | null = controlValue instanceof File ? controlValue : controlValue?.[0];
    
    if (!file) {
      this.toastr.warning('Chưa chọn file!', 'Cảnh báo');
      return;
    }

    this.evaluationService.importExcel(file, classroomId, subjectId, semesterId).subscribe({

      next: ({ data, userMessage }) => {
        const successCount = data?.successCount ?? 0;
        const failedCount = data?.failedCount ?? 0;
        const message = (userMessage as string) || `Import hoàn tất: ${successCount} bản ghi thành công, ${failedCount} bản ghi lỗi`;

        if (failedCount > 0) {
          this.toastr.warning(message, 'Kết nạp hoàn tất');
          if (data?.hasErrorFile && data.errorFileToken) {
            this.dialog.confirm(
              {
                title: 'Tải file lỗi',
                message: 'Có bản ghi import lỗi. Bạn có muốn tải file lỗi để chỉnh sửa không?',
              },
              (confirmed?: boolean) => {
                if (confirmed) {
                  this.downloadErrorFile(data.errorFileToken, data.errorFileName || 'evaluation_import_errors.xlsx');
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
        this.toastr.error(error?.error?.userMessage ?? error?.error?.message ?? 'Kết nạp dữ liệu thất bại', 'Thất bại');
      },
    });
  }

  private downloadErrorFile(token: string, fileName: string): void {
    this.evaluationService.downloadImportError(token).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
      },
      error: () => {
        this.toastr.error('Không tải được file lỗi', 'Thất bại');
      },
    });
  }
}
