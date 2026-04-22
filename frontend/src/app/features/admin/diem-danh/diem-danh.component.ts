import { CommonModule } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { IconComponent } from '@components/app-icon/app-icon.component';
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
  DiemDanhItemSaveRequest,
  DiemDanhKhoiNhomItem,
  DiemDanhLopItem,
  DiemDanhNgayHocSinh,
  DiemDanhRowViewModel,
} from '@app/model/admin/diem-danh.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { DiemDanhService } from '@app/service/admin/diem-danh.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';

@Component({
  selector: 'diem-danh-page',
  standalone: true,
  templateUrl: './diem-danh.component.html',
  styleUrls: ['./diem-danh.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, IconComponent, ...MATERIAL_MODULE, ...FORM_CONTROL_MODULE],
})
export class DiemDanhComponent extends ComponentBaseAbstract {
  readonly menuCode = 'ATTENDANCE';

  readonly attendanceStatusOptions: IOptions[] = [
    { value: 'P', label: 'P - Nghỉ có phép' },
    { value: 'K', label: 'K - Nghỉ không phép' },
    { value: 'C', label: 'C - Có mặt' },
    { value: 'X', label: 'X - Trường hợp khác' },
  ];

  readonly sessionTypeOptions: IOptions[] = [
    { value: 'MORNING', label: 'Sáng' },
    { value: 'AFTERNOON', label: 'Chiều' },
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
    DATE_CONTROL({
      controlName: 'attendanceDate',
      label: 'Ngày',
      placeholder: 'Chọn ngày',
      required: true,
      showLabel: true,
    }),
  ];

  gradeClassGroups: DiemDanhKhoiNhomItem[] = [];
  selectedGradeId?: string;
  selectedClassId?: string;
  selectedClassName = '';
  rows: DiemDanhRowViewModel[] = [];
  monthlySheetLoaded = false;
  currentSchoolYear?: NamHocOptionResponse;

  get currentUnitId(): string | number | undefined {
    const unitId = this.authService.currentUser?.unit?.id;
    return unitId == null || unitId === '' ? undefined : unitId;
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
    this.findFormControl(this.$formItem, 'sessionType').options = this.sessionTypeOptions;
    this.form.patchValue(
      {
        sessionType: 'MORNING',
        month: this.toMonthInput(new Date()),
        attendanceDate: this.toDateInput(new Date()),
      },
      { emitEvent: false }
    );
    this.loadInitialData();
    this.bindFilters();
  }

  selectClass(group: DiemDanhKhoiNhomItem, classroom: DiemDanhLopItem): void {
    this.selectedGradeId = `${group.gradeLevelId}`;
    this.selectedClassId = `${classroom.id}`;
    this.selectedClassName = classroom.name;
    this.reloadAttendanceData();
  }

  updateRowStatus(studentId: string | number, status: string | number | boolean): void {
    this.rows = this.rows.map((row) =>
      `${row.studentId}` === `${studentId}`
        ? { ...row, attendanceStatus: `${status ?? ''}` }
        : row
    );
  }

  updateRowNote(studentId: string | number, note: string): void {
    this.rows = this.rows.map((row) =>
      `${row.studentId}` === `${studentId}` ? { ...row, note } : row
    );
  }

  saveRow(row: DiemDanhRowViewModel): void {
    const payload = this.buildSavePayload(row);
    if (!payload) return;

    this.diemDanhService.saveAttendance(payload).subscribe({
      next: () => {
        this.toastr.success(`Đã lưu điểm danh cho ${row.fullName}`, 'Thành công');
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu điểm danh thất bại',
          'Thất bại'
        );
      },
    });
  }

  saveAll(): void {
    const payload = this.rows
      .map((row) => this.buildSavePayload(row))
      .filter((item): item is DiemDanhItemSaveRequest => !!item);

    if (!payload.length) {
      this.toastr.warning('Chưa có dữ liệu điểm danh để lưu', 'Cảnh báo');
      return;
    }

    this.diemDanhService.saveAttendanceBulk(payload).subscribe({
      next: () => {
        this.toastr.success('Đã lưu danh sách điểm danh', 'Thành công');
        this.reloadAttendanceData();
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu danh sách điểm danh thất bại',
          'Thất bại'
        );
      },
    });
  }

  private loadInitialData(): void {
    if (!this.currentUnitId) {
      this.toastr.warning('Không xác định được đơn vị đăng nhập', 'Cảnh báo');
      return;
    }

    this.namHocService.getCurrent().subscribe({
      next: ({ data }) => {
        this.currentSchoolYear = data;
        if (!data?.id) {
          this.toastr.warning('Không xác định được năm học hiện tại', 'Cảnh báo');
          return;
        }
        this.loadGradeClassGroups(data.id);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Không tải được năm học hiện tại',
          'Thất bại'
        );
      },
    });
  }

  private bindFilters(): void {
    this.form.get('sessionType')?.valueChanges.subscribe(() => {
      this.reloadAttendanceData();
    });

    this.form.get('attendanceDate')?.valueChanges.subscribe((value) => {
      if (value) {
        this.form.get('month')?.setValue(this.toMonthInput(value), { emitEvent: false });
      }
      this.reloadAttendanceData();
    });

    this.form.get('month')?.valueChanges.subscribe((value) => {
      if (!value) return;
      this.loadMonthlySheet();
    });
  }

  private loadGradeClassGroups(schoolYearId: string | number): void {
    this.diemDanhService.getGradeClassGroups(this.currentUnitId!, schoolYearId).subscribe({
      next: ({ data }) => {
        this.gradeClassGroups = data ?? [];
        const firstGroup = this.gradeClassGroups[0];
        const firstClass = firstGroup?.classes?.[0];
        if (firstGroup && firstClass) {
          this.selectClass(firstGroup, firstClass);
        }
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Không tải được danh sách khối và lớp',
          'Thất bại'
        );
      },
    });
  }

  private reloadAttendanceData(): void {
    if (!this.selectedClassId) return;
    this.loadMonthlySheet();
    this.loadDailySheet();
  }

  private loadMonthlySheet(): void {
    if (!this.selectedClassId) return;

    const month = `${this.form.get('month')?.value ?? ''}`;
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    if (!month || !sessionType) return;

    this.diemDanhService.getMonthlySheet(this.selectedClassId, month, sessionType).subscribe({
      next: () => {
        this.monthlySheetLoaded = true;
      },
      error: () => {
        this.monthlySheetLoaded = false;
      },
    });
  }

  private loadDailySheet(): void {
    if (!this.selectedClassId) return;

    const attendanceDate = this.normalizeDateParam(this.form.get('attendanceDate')?.value);
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    if (!attendanceDate || !sessionType) return;

    this.diemDanhService
      .getDailySheet(this.selectedClassId, attendanceDate, sessionType)
      .subscribe({
        next: ({ data }) => {
          const students = data?.students ?? [];
          if (students.length) {
            this.rows = students.map((student) => this.mapStudentRow(student));
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

    this.diemDanhService.getStudentsByClassroom(this.selectedClassId).subscribe({
      next: ({ data }) => {
        this.rows = (data ?? []).map((student) => ({
          studentId: student.id,
          fullName: student.name,
          attendanceStatus: '',
          note: '',
        }));
      },
      error: (error) => {
        this.rows = [];
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Không tải được danh sách học sinh',
          'Thất bại'
        );
      },
    });
  }

  private mapStudentRow(student: DiemDanhNgayHocSinh): DiemDanhRowViewModel {
    return {
      studentId: student.studentId,
      studentCode: student.studentCode ?? '',
      fullName: student.fullName,
      attendanceStatus: student.attendanceStatus ?? '',
      note: student.note ?? '',
    };
  }

  private buildSavePayload(row: DiemDanhRowViewModel): DiemDanhItemSaveRequest | null {
    const attendanceDate = this.normalizeDateParam(this.form.get('attendanceDate')?.value);
    const sessionType = `${this.form.get('sessionType')?.value ?? ''}`;
    const classroomId = Number(this.selectedClassId ?? 0);
    const studentId = Number(row.studentId ?? 0);
    const status = `${row.attendanceStatus ?? ''}`.trim();

    if (!classroomId || !studentId || !attendanceDate || !sessionType || !status) {
      return null;
    }

    return {
      classroomId,
      studentId,
      attendanceDate,
      sessionType,
      status,
      note: row.note?.trim() || '',
    };
  }

  private normalizeDateParam(value: unknown): string {
    const stringValue = `${value ?? ''}`.trim();
    return stringValue ? stringValue.slice(0, 10) : '';
  }

  private toMonthInput(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
  }

  private toDateInput(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
  }
}
