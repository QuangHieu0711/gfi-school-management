/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { HocSinhResponse } from '@app/model/admin/hoc-sinh.model';
import { HocBaExportService } from '@app/service/admin/hoc-ba-export.service';


export type HocBaExportType = 'EXCEL' | 'PDF';

export interface DialogExportHocBaData {
  students: HocSinhResponse[];
}

@Component({
  selector: 'dialog-export-hoc-ba',
  standalone: true,
  templateUrl: './dialog-export-hoc-ba.component.html',
  styleUrls: ['./dialog-export-hoc-ba.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppDialogComponent,
    ...MATERIAL_MODULE,
  ],
})
export class DialogExportHocBaComponent extends ComponentBaseAbstract {
  readonly title = 'Xuất học bạ';
  selectedType: HocBaExportType = 'PDF';
  isExporting = false;

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogExportHocBaComponent>,
    private readonly hocBaExportService: HocBaExportService,
    @Inject(MAT_DIALOG_DATA) public data: DialogExportHocBaData
  ) {
    super(injector);
  }

  get students(): HocSinhResponse[] {
    return this.data?.students ?? [];
  }

  get studentCount(): number {
    return this.students.length;
  }

  selectType(type: HocBaExportType): void {
    this.selectedType = type;
  }

  onExport(): void {
    if (!this.students.length) {
      this.toastr.warning('Không có học sinh nào được chọn', 'Cảnh báo');
      return;
    }

    this.isExporting = true;

    try {
      if (this.selectedType === 'PDF') {
        this.hocBaExportService.exportToPdf(this.students);
        this.toastr.success('Đang mở học bạ PDF trong tab mới...', 'Thành công');
      } else {
        this.hocBaExportService.exportToExcel(this.students);
        this.toastr.success('Xuất học bạ Excel thành công', 'Thành công');
      }
      this.dialogRef.close(true);
    } catch (err: any) {
      this.toastr.error(
        err?.message ?? 'Xuất học bạ thất bại',
        'Lỗi'
      );
    } finally {
      this.isExporting = false;
    }
  }
}
