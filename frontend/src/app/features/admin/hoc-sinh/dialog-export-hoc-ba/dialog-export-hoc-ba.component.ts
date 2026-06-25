/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  HocSinhReportCardExportRequest,
  HocSinhResponse,
} from '@app/model/admin/hoc-sinh.model';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import {
  defaultExportFileName,
  saveBlobAsFile,
} from '@utils/file-util';

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
    private readonly hocSinhService: HocSinhService,
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

    const studentIds = this.students
      .map((student) => student.id)
      .filter((id): id is number | string => id !== null && id !== undefined);

    if (!studentIds.length) {
      this.toastr.error('Không lấy được danh sách học sinh để xuất học bạ', 'Lỗi');
      return;
    }

    this.isExporting = true;

    const payload: HocSinhReportCardExportRequest = {
      studentIds,
      exportType: this.selectedType,
    };

    this.hocSinhService.exportReportCards(payload).subscribe({
      next: (res: any) => {
        const blob = this.extractBlob(res);
        if (!blob) {
          this.toastr.error(
            'Xuất học bạ thất bại: dữ liệu tải về không hợp lệ',
            'Lỗi'
          );
          return;
        }

        const ext = this.selectedType === 'PDF' ? 'pdf' : 'xlsx';
        const fallbackName = defaultExportFileName('hoc-ba-hoc-sinh', ext);
        const disposition = this.getHeader(res, 'content-disposition');
        const fileName = this.getFileNameFromDisposition(
          disposition,
          fallbackName
        );

        saveBlobAsFile(blob, fileName);
        this.toastr.success(
          `Tải xuống ${this.selectedType} thành công`,
          'Xuất học bạ'
        );
        this.dialogRef.close(true);
      },
      error: () => {
        this.toastr.error('Xuất học bạ thất bại', 'Lỗi');
      },
      complete: () => {
        this.isExporting = false;
      },
    });
  }

  private extractBlob(res: any): Blob | null {
    if (res instanceof Blob) return res;
    if (res?.body instanceof Blob) return res.body;
    if (res?.data instanceof Blob) return res.data;
    return null;
  }

  private getHeader(res: any, headerName: string): string | null {
    return res?.headers?.get?.(headerName) ?? null;
  }

  private getFileNameFromDisposition(
    disposition: string | null,
    fallback: string
  ): string {
    if (!disposition) return fallback;

    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) {
      return decodeURIComponent(utf8Match[1]);
    }

    const asciiMatch = disposition.match(/filename=\"?([^\";]+)\"?/i);
    return asciiMatch?.[1] ?? fallback;
  }
}
