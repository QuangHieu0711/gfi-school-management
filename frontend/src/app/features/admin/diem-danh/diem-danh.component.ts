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

import {
  DiemDanhHocSinhOption,
  DiemDanhItemSaveRequest,
  DiemDanhKhoiNhomItem,
  DiemDanhLopItem,
  DiemDanhThangHocSinhApi,
  DiemDanhThangRowViewModel,
} from '@app/model/admin/diem-danh.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { DiemDanhService } from '@app/service/admin/diem-danh.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';

/** Ngày trong tháng để hiển thị cột */
export interface CalendarDay {
  date: string; // 'YYYY-MM-DD'
  dayNum: number; // 1-31
  dayOfWeek: string; // T2, T3, ... CN
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

  /** Các ngày trong tháng hiện tại */
  calendarDays: CalendarDay[] = [];

  /** Rows học sinh – mỗi row có statusMap[date] = 'P'|'K'|'X'|'' */
  monthRows: DiemDanhThangRowViewModel[] = [];

  /** Ngày hôm nay dạng 'YYYY-MM-DD' để highlight cột */
  readonly todayStr: string = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${`${d.getMonth() + 1}`.padStart(2, '0')}-${`${d.getDate()}`.padStart(2, '0')}`;
  })();

  /** Danh sách trạng thái hợp lệ */
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

  // ─── Sidebar ─────────────────────────────────────────────────────────────
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

  // ─── Cell editing ────────────────────────────────────────────────────────
  onCellInput(studentId: string | number, date: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const raw = input.value.toUpperCase().trim();
    const status = this.validStatuses.has(raw) ? raw : '';
    input.value = status; // normalize immediately in DOM
    this.setCellStatus(studentId, date, status);
  }

  onCellKeydown(
    event: KeyboardEvent,
    studentId: string | number,
    date: string
  ): void {
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

  // ─── Save ────────────────────────────────────────────────────────────────
  saveAll(): void {
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    const classroomId = Number(this.selectedClassId ?? 0);

    if (!classroomId || !sessionType) {
      this.toastr.warning('Chưa chọn lớp hoặc buổi', 'Cảnh báo');
      return;
    }

    const payload: DiemDanhItemSaveRequest[] = [];

    for (const row of this.monthRows) {
      for (const day of this.calendarDays) {
        const status = (row.statusMap[day.date] ?? '').trim();
        if (!status) continue;
        payload.push({
          classroomId,
          studentId: Number(row.studentId),
          attendanceDate: day.date,
          sessionType,
          status,
        });
      }
    }

    if (!payload.length) {
      this.toastr.warning('Chưa nhập trạng thái điểm danh nào', 'Cảnh báo');
      return;
    }

    this.diemDanhService.saveAttendanceBulk(payload).subscribe({
      next: () => {
        this.toastr.success('Đã lưu điểm danh cả tháng', 'Thành công');
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
    this.toastr.info('Tính năng xuất Excel đang được phát triển', 'Thông báo');
  }

  // ─── Helper: calendar ─────────────────────────────────────────────────────
  buildCalendarDays(month: string): void {
    if (!month) return;
    const [yearStr, monthStr] = month.split('-');
    const year = Number(yearStr);
    const monthIndex = Number(monthStr) - 1;
    const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();

    this.calendarDays = Array.from({ length: daysInMonth }, (_, i) => {
      const d = new Date(year, monthIndex, i + 1);
      const dow = d.getDay(); // 0=Sun
      return {
        date: `${year}-${`${monthIndex + 1}`.padStart(2, '0')}-${`${i + 1}`.padStart(2, '0')}`,
        dayNum: i + 1,
        dayOfWeek: DAY_LABELS[dow],
        isWeekend: dow === 0 || dow === 6,
      };
    });
  }

  private calcSummary(statusMap: Record<string, string>): {
    countP: number;
    countK: number;
    countX: number;
    totalAbsent: number;
  } {
    let p = 0,
      k = 0,
      x = 0;
    for (const s of Object.values(statusMap)) {
      if (s === 'P') p++;
      else if (s === 'K') k++;
      else if (s === 'X') x++;
    }
    return { countP: p, countK: k, countX: x, totalAbsent: p + k + x };
  }

  // ─── Data loading ─────────────────────────────────────────────────────────
  private loadInitialData(): void {
    if (!this.currentUnitId) {
      this.toastr.warning('Không xác định được đơn vị đăng nhập', 'Cảnh báo');
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

  loadMonthlySheet(): void {
    if (!this.selectedClassId) return;
    const month = this.currentMonth;
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    if (!month || !sessionType) return;

    this.diemDanhService
      .getMonthlySheet(this.selectedClassId, month, sessionType)
      .subscribe({
        next: ({ data }) => {
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
    const statusMap: Record<string, string> = {};
    for (const att of student.attendances ?? []) {
      statusMap[att.date] = att.status ?? '';
    }
    return {
      studentId: student.studentId ?? 0,
      studentCode: student.studentCode ?? '',
      fullName: student.fullName ?? '',
      statusMap,
      ...this.calcSummary(statusMap),
    };
  }

  // ─── Utils ────────────────────────────────────────────────────────────────
  private toMonthInput(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
  }

  getCellValue(row: DiemDanhThangRowViewModel, date: string): string {
    return row.statusMap[date] ?? '';
  }

  trackByDate(_: number, day: CalendarDay): string {
    return day.date;
  }

  trackByStudent(_: number, row: DiemDanhThangRowViewModel): string {
    return `${row.studentId}`;
  }
}
