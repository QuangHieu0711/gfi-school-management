/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ComponentBaseAbstract } from '@layout';
import {
  DATE_CONTROL,
  FormType,
  IOptions,
  SELECT_CONTROL,
} from '@model/form-control.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { AuthService } from '@service';
import { defaultExportFileName, saveBlobAsFile } from '@utils/file-util';

import {
  DiemDanhBulkItemSaveRequest,
  DiemDanhExportRequest,
  DiemDanhImportRequest,
  DiemDanhHocSinhOption,
  DiemDanhKhoiNhomItem,
  DiemDanhLopItem,
  DiemDanhThangHocSinhApi,
  DiemDanhThangRowViewModel,
} from '@app/model/admin/diem-danh.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { DiemDanhService } from '@app/service/admin/diem-danh.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { DialogImportDiemDanhComponent } from './dialog-import/dialog-import.component';

export interface CalendarDay {
  date: string;
  dayNum: number;
  dayOfWeek: string;
  isWeekend: boolean;
}

const DAY_LABELS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

@Component({
  selector: 'diem-danh-page',
  standalone: true,
  templateUrl: './diem-danh.component.html',
  styleUrls: ['./diem-danh.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class DiemDanhComponent extends ComponentBaseAbstract {
  readonly menuCode = 'ATTENDANCE';

  readonly sessionTypeOptions: IOptions[] = [
    { value: 'SANG', label: 'Sáng' },
    { value: 'CHIEU', label: 'Chiều' },
  ];

  readonly $formItem: FormType[] = [
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
  ];

  gradeClassGroups: DiemDanhKhoiNhomItem[] = [];
  expandedGradeIds = new Set<string>();
  selectedGradeId?: string;
  selectedClassId?: string;
  selectedClassName = '';
  currentSchoolYear?: NamHocOptionResponse;
  calendarDays: CalendarDay[] = [];
  monthRows: DiemDanhThangRowViewModel[] = [];

  readonly todayStr: string = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${`${d.getMonth() + 1}`.padStart(2, '0')}-${`${d.getDate()}`.padStart(2, '0')}`;
  })();

  readonly validStatuses = new Set(['P', 'K', 'X', 'C', '']);

  get currentUnitId(): string | number | undefined {
    const unitId = this.authService.currentUser?.unit?.id;
    return unitId == null || unitId === '' ? undefined : unitId;
  }

  get currentMonth(): string {
    return `${this.form?.get('month')?.value ?? ''}`;
  }

  constructor(
    protected override injector: Injector,
    private readonly diemDanhService: DiemDanhService,
    private readonly namHocService: NamHocService,
    private readonly authService: AuthService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.findFormControl(this.$formItem, 'sessionType').options =
      this.sessionTypeOptions;

    const now = new Date();
    this.form.patchValue(
      {
        sessionType: 'SANG',
        month: this.toMonthInput(now),
      },
      { emitEvent: false }
    );

    this.buildCalendarDays(this.toMonthInput(now));
    this.loadInitialData();
    this.bindFilters();
  }

  selectClass(group: DiemDanhKhoiNhomItem, classroom: DiemDanhLopItem): void {
    this.selectedGradeId = `${group.gradeLevelId}`;
    this.selectedClassId = `${classroom.id}`;
    this.selectedClassName = classroom.name;
    this.expandedGradeIds.add(`${group.gradeLevelId}`);
    this.loadMonthlySheet();
  }

  toggleGrade(groupId: string | number): void {
    const id = `${groupId}`;
    if (this.expandedGradeIds.has(id)) {
      this.expandedGradeIds.delete(id);
      return;
    }
    this.expandedGradeIds.add(id);
  }

  isGradeExpanded(groupId: string | number): boolean {
    return this.expandedGradeIds.has(`${groupId}`);
  }

  onCellInput(studentId: string | number, date: string, event: Event): void {
    if (this.isFutureDate(date)) {
      (event.target as HTMLInputElement).value = this.getRowStatus(studentId, date);
      return;
    }

    const input = event.target as HTMLInputElement;
    const raw = input.value.toUpperCase().trim();
    const status = this.validStatuses.has(raw) ? raw : '';
    input.value = status;
    this.setCellStatus(studentId, date, status);
  }

  onCellKeydown(
    event: KeyboardEvent,
    studentId: string | number,
    date: string
  ): void {
    if (this.isFutureDate(date)) {
      event.preventDefault();
      return;
    }

    const key = event.key.toUpperCase();
    if (['P', 'K', 'X', 'C'].includes(key)) {
      event.preventDefault();
      (event.target as HTMLInputElement).value = key;
      this.setCellStatus(studentId, date, key);
    } else if (event.key === 'Delete' || event.key === 'Backspace') {
      event.preventDefault();
      (event.target as HTMLInputElement).value = '';
      this.setCellStatus(studentId, date, '');
    }
  }

  saveAll(): void {
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    const classroomId = Number(this.selectedClassId ?? 0);

    if (!classroomId || !sessionType) {
      this.toastr.warning('Chưa chọn lớp hoặc buổi', 'Cảnh báo');
      return;
    }

    const items: DiemDanhBulkItemSaveRequest[] = [];

    for (const day of this.calendarDays) {
      if (this.isFutureDate(day.date)) continue;

      const students = this.monthRows
        .map((row) => {
          const status = (row.statusMap[day.date] ?? '').trim();
          if (!status) return null;

          return {
            studentId: Number(row.studentId),
            studentName: row.fullName,
            status,
          };
        })
        .filter((student): student is NonNullable<typeof student> => !!student);

      if (!students.length) continue;

      items.push({
        attendanceDate: day.date,
        students,
      });
    }

    if (!items.length) {
      this.toastr.warning('Chưa nhập trạng thái điểm danh nào', 'Cảnh báo');
      return;
    }

    this.diemDanhService
      .saveAttendanceBulk({
        classroomId,
        sessionType,
        items,
      })
      .subscribe({
        next: () => {
          this.toastr.success('Lưu điểm danh thành công', 'Thành công');
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Lưu điểm danh thất bại',
            'Thất bại'
          );
        },
      });
  }

  exportExcel(): void {
    this.exportFile('EXCEL');
  }

  exportPdf(): void {
    this.exportFile('PDF');
  }

  importAttendance(): void {
    const classroomId = Number(this.selectedClassId ?? 0);
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    const [yearStr, monthStr] = this.currentMonth.split('-');
    const year = Number(yearStr);
    const month = Number(monthStr);

    if (!classroomId || !sessionType || !year || !month) {
      this.toastr.warning('Chưa chọn đủ lớp, buổi hoặc tháng', 'Cảnh báo');
      return;
    }

    const data: DiemDanhImportRequest = {
      classroomId,
      year,
      month,
      sessionType,
    };

    this.dialog.componentDialog(
      DialogImportDiemDanhComponent,
      {
        width: '900px',
        data,
      },
      (result?: {
        refresh?: boolean;
        month?: string;
        sessionType?: string;
      }) => {
        if (result?.sessionType || result?.month) {
          this.form.patchValue(
            {
              sessionType: result.sessionType ?? sessionType,
              month: result.month ?? this.currentMonth,
            },
            { emitEvent: false }
          );
        }

        if (result?.month) {
          this.buildCalendarDays(result.month);
        }

        if (result?.refresh) {
          this.loadMonthlySheet();
        }
      }
    );
  }

  cancelChanges(): void {
    this.loadMonthlySheet();
  }

  buildCalendarDays(month: string): void {
    if (!month) return;
    const [yearStr, monthStr] = month.split('-');
    const year = Number(yearStr);
    const monthIndex = Number(monthStr) - 1;
    const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();

    this.calendarDays = Array.from({ length: daysInMonth }, (_, i) => {
      const d = new Date(year, monthIndex, i + 1);
      const dow = d.getDay();
      return {
        date: `${year}-${`${monthIndex + 1}`.padStart(2, '0')}-${`${i + 1}`.padStart(2, '0')}`,
        dayNum: i + 1,
        dayOfWeek: DAY_LABELS[dow],
        isWeekend: dow === 0 || dow === 6,
      };
    });
  }

  loadMonthlySheet(): void {
    if (!this.selectedClassId) return;
    const month = this.currentMonth;
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    if (!month || !sessionType) return;

    this.diemDanhService
      .getMonthlySheet(this.selectedClassId, month, sessionType)
      .subscribe({
        next: ({ data }) => {
          this.selectedClassName =
            data?.classroomName?.trim() || this.selectedClassName;

          const apiStudents: DiemDanhThangHocSinhApi[] =
            (data?.students as DiemDanhThangHocSinhApi[]) ?? [];
          if (apiStudents.length) {
            this.monthRows = apiStudents.map((s) => this.mapMonthRow(s));
            return;
          }
          this.loadStudentsFallback();
        },
        error: () => {
          this.loadStudentsFallback();
        },
      });
  }

  getCellValue(row: DiemDanhThangRowViewModel, date: string): string {
    return row.statusMap[date] ?? '';
  }

  isFutureDate(date: string): boolean {
    return date > this.todayStr;
  }

  trackByDate(_: number, day: CalendarDay): string {
    return day.date;
  }

  trackByStudent(_: number, row: DiemDanhThangRowViewModel): string {
    return `${row.studentId}`;
  }

  private setCellStatus(
    studentId: string | number,
    date: string,
    status: string
  ): void {
    this.monthRows = this.monthRows.map((row) => {
      if (`${row.studentId}` !== `${studentId}`) return row;
      const newMap = { ...row.statusMap, [date]: status };
      return { ...row, statusMap: newMap, ...this.calcSummary(newMap) };
    });
  }

  private calcSummary(statusMap: Record<string, string>): {
    countP: number;
    countK: number;
    countX: number;
    totalAbsent: number;
  } {
    let p = 0;
    let k = 0;
    let x = 0;
    for (const s of Object.values(statusMap)) {
      if (s === 'P') p++;
      else if (s === 'K') k++;
      else if (s === 'X') x++;
    }
    return { countP: p, countK: k, countX: x, totalAbsent: p + k + x };
  }

  private loadInitialData(): void {
    if (!this.currentUnitId) {
      this.toastr.error('Không xác định được đơn vị đăng nhập', 'Thất bại');
      return;
    }

    this.namHocService.getCurrent().subscribe({
      next: ({ data }) => {
        this.currentSchoolYear = data;
        if (!data?.id) {
          this.toastr.warning(
            'Không xác định được năm học hiện tại',
            'Cảnh báo'
          );
          return;
        }
        this.loadGradeClassGroups(data.id);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được năm học hiện tại',
          'Thất bại'
        );
      },
    });
  }

  private bindFilters(): void {
    this.form.get('sessionType')?.valueChanges.subscribe(() => {
      this.loadMonthlySheet();
    });

    this.form.get('month')?.valueChanges.subscribe((value) => {
      if (!value) return;
      this.buildCalendarDays(`${value}`);
      this.loadMonthlySheet();
    });
  }

  private loadGradeClassGroups(schoolYearId: string | number): void {
    this.diemDanhService
      .getGradeClassGroups(this.currentUnitId!, schoolYearId)
      .subscribe({
        next: ({ data }) => {
          this.gradeClassGroups = data ?? [];
          const firstGroup = this.gradeClassGroups[0];
          const firstClass = firstGroup?.classes?.[0];
          if (firstGroup && firstClass) {
            this.expandedGradeIds = new Set([`${firstGroup.gradeLevelId}`]);
            this.selectClass(firstGroup, firstClass);
          }
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được danh sách khối và lớp',
            'Thất bại'
          );
        },
      });
  }

  private loadStudentsFallback(): void {
    if (!this.selectedClassId) return;
    this.diemDanhService
      .getStudentsByClassroom(this.selectedClassId)
      .subscribe({
        next: ({ data }) => {
          this.monthRows = (data ?? []).map((s: DiemDanhHocSinhOption) => ({
            studentId: s.id,
            studentCode: s.code ?? '',
            fullName: s.name,
            statusMap: {},
            countP: 0,
            countK: 0,
            countX: 0,
            totalAbsent: 0,
          }));
        },
        error: (error) => {
          this.monthRows = [];
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được danh sách học sinh',
            'Thất bại'
          );
        },
      });
  }

  private mapMonthRow(
    student: DiemDanhThangHocSinhApi
  ): DiemDanhThangRowViewModel {
    const statusMap = { ...(student.attendance ?? {}) };
    return {
      studentId: student.studentId ?? 0,
      studentCode: student.studentCode ?? '',
      fullName: student.studentName ?? '',
      statusMap,
      ...this.calcSummary(statusMap),
    };
  }

  private toMonthInput(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
  }

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const classroomId = Number(this.selectedClassId ?? 0);
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    const [yearStr, monthStr] = this.currentMonth.split('-');
    const year = Number(yearStr);
    const month = Number(monthStr);

    if (!classroomId || !sessionType || !year || !month) {
      this.toastr.warning('Chưa chọn đủ lớp, buổi hoặc tháng', 'Cảnh báo');
      return;
    }

    const payload: DiemDanhExportRequest = {
      classroomId,
      year,
      month,
      sessionType,
      exportType,
    };

    this.diemDanhService.export(payload).subscribe({
      next: (res: any) => {
        const blob = this.extractBlob(res);
        if (!blob) {
          this.toastr.error(
            `Xuất ${exportType} thất bại: Dữ liệu không hợp lệ`,
            'Lỗi'
          );
          return;
        }

        const ext = exportType === 'PDF' ? 'pdf' : 'xlsx';
        const fallbackName = defaultExportFileName('diem-danh', ext);
        const disposition = this.getHeader(res, 'content-disposition');
        const fileName = this.getFileNameFromDisposition(
          disposition,
          fallbackName
        );

        saveBlobAsFile(blob, fileName);
        this.toastr.success(
          `Tải xuống ${exportType} thành công`,
          `Xuất ${exportType}`
        );
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            `Xuất ${exportType} thất bại`,
          'Lỗi'
        );
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
    if (res?.headers?.get) return res.headers.get(headerName);

    const headers = res?.headers;
    if (headers && typeof headers === 'object') {
      const key = headerName.toLowerCase();
      return headers[headerName] ?? headers[key] ?? null;
    }

    return null;
  }

  private getFileNameFromDisposition(
    disposition: string | null,
    fallbackName: string
  ): string {
    if (!disposition) return fallbackName;

    const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^";]+)/i);
    if (!match?.[1]) return fallbackName;

    const rawFileName = match[1].trim();

    try {
      return this.decodeMimeFileName(decodeURIComponent(rawFileName));
    } catch {
      return this.decodeMimeFileName(rawFileName);
    }
  }

  private decodeMimeFileName(fileName: string): string {
    const mimeMatch = fileName.match(/^=\?UTF-8\?Q\?(.+)\?=$/i);
    if (!mimeMatch?.[1]) return fileName;

    const normalized = mimeMatch[1].replace(/_/g, ' ');
    const decoded = normalized.replace(/=([0-9A-F]{2})/gi, '%$1');

    try {
      return decodeURIComponent(decoded);
    } catch {
      return fileName;
    }
  }

  private getRowStatus(studentId: string | number, date: string): string {
    return (
      this.monthRows.find((row) => `${row.studentId}` === `${studentId}`)?.statusMap[
        date
      ] ?? ''
    );
  }
}
