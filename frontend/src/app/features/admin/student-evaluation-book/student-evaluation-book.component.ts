/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MtxSelectModule } from '@ng-matero/extensions/select';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { filter, take, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { FormType, SELECT_CONTROL } from '@model/form-control.model';
import { ComponentBaseAbstract } from '@layout';
import { COMMON_TABLE_KEY, TableConfig } from '@model/table.model';
import {
  DiemDanhKhoiNhomItem,
  DiemDanhLopItem,
} from '@app/model/admin/diem-danh.model';
import { DiemDanhService } from '@app/service/admin/diem-danh.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';
import { LopMonHocDetailSubjectResponse } from '@app/model/admin/lop-mon-hoc.model';
import { LopMonHocService } from '@app/service/admin/lop-mon-hoc.service';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { ID_TYPE } from '@model/response.model';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { AuthService } from '@service';
import { EvaluationService } from '@app/service/admin/evaluation.service';
import { EvaluationBulkSaveRequest, EvaluationGenerateCommentRequest, EvaluationBulkGenerateCommentRequest, EvaluationBulkGenerateCommentItem } from '@app/model/admin/evaluation.model';

interface TeacherClassAssignmentResponse {
  classId?: string | number;
  classroomId?: string | number;
  id?: string | number;
  className?: string;
  classCode?: string;
  name?: string;
  gradeLevelId?: string | number;
  gradeId?: string | number;
  gradeLevelName?: string;
  gradeName?: string;
  gradeNumber?: number;
  subjects?: LopMonHocDetailSubjectResponse[];
  classes?: DiemDanhLopItem[];
}

@Component({
  selector: 'student-evaluation-book',
  standalone: true,
  templateUrl: './student-evaluation-book.component.html',
  styleUrls: ['./student-evaluation-book.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    MtxSelectModule,
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class StudentEvaluationBookComponent extends ComponentBaseAbstract {
  @ViewChild('nameTpl', { static: true }) nameTpl!: TemplateRef<unknown>;
  @ViewChild('middleTermTpl', { static: true })
  middleTermTpl!: TemplateRef<unknown>;
  @ViewChild('commentGKTpl', { static: true })
  commentGKTpl!: TemplateRef<unknown>;
  @ViewChild('finalTermTpl', { static: true })
  finalTermTpl!: TemplateRef<unknown>;
  @ViewChild('commentFinalTpl', { static: true })
  commentFinalTpl!: TemplateRef<unknown>;

  readonly termOptions = [
    { value: 'T', label: 'T' },
    { value: 'H', label: 'H' },
    { value: 'C', label: 'C' },
  ];

  readonly key = {
    CLASSROOM_ID: 'classroomId',
    SEMESTER_ID: 'semesterId',
    INPUT_ROUND: 'inputRound',
  } as const;

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: this.key.SEMESTER_ID,
      placeholder: 'Chọn học kỳ',
      required: false,
      listOption: [],
      clearable: true,
    }),
  ];

  tableConfig: TableConfig = {
    hasFilterPanel: true,
    hasFilterPanelButton: false,
    hasExport: true,
    showPaginator: false,
  };
  columns: MtxGridColumn[] = [];
  dataSource: any[] = [];

  // Class picker / subject UI state
  gradeClassGroups: DiemDanhKhoiNhomItem[] = [];
  expandedGradeIds = new Set<string>();
  selectedClassId?: string | number;
  selectedClassName = '';
  subjects: LopMonHocDetailSubjectResponse[] = [];
  selectedSubjectId?: any;
  currentSchoolYear?: NamHocOptionResponse;

  get selectedSubjectName(): string {
    if (!this.selectedSubjectId || !this.subjects.length) return '';
    const found = this.subjects.find(
      (s) => s.subjectId === this.selectedSubjectId
    );
    return found?.subjectName ?? '';
  }

  private subjectsByClassId = new Map<
    string,
    LopMonHocDetailSubjectResponse[]
  >();

  constructor(
    protected override injector: Injector,
    private readonly diemDanhService: DiemDanhService,
    private readonly phanCongService: PhanCongGiangDayService,
    private readonly lopMonHocService: LopMonHocService,
    private readonly namHocService: NamHocService,
    private readonly hocKyService: HocKyService,
    private readonly authService: AuthService,
    private readonly evaluationService: EvaluationService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    // Initialize form controls and load class/subject data
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.loadInitialData();

    // Start with empty data – will be populated from API when class is selected
    this.dataSource = [];
    this.dataSourceTotal = 0;

    this.columns = [
      { header: 'STT', class: 'text-center', field: COMMON_TABLE_KEY.STT, width: '50px' },
      {
        header: 'Họ và tên',
        field: 'fullName',
        width: '210px',
        cellTemplate: this.nameTpl,
      },
      {
        header: 'Giữa học kỳ',
        field: 'middleTerm',
        width: '65px',
        class: 'text-center',
        cellTemplate: this.middleTermTpl,
      },
      {
        header: 'Nhận xét GK',
        field: 'commentGK',
        cellTemplate: this.commentGKTpl,
      },
      {
        header: 'Cuối học kỳ',
        field: 'finalTerm',
        width: '65px',
        class: 'text-center',
        cellTemplate: this.finalTermTpl,
      },
      {
        header: 'Nhận xét',
        field: 'commentFinal',
        cellTemplate: this.commentFinalTpl,
      },
    ];
  }

  filterData(pageChangeEvent?: any) {
    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;
    this.loadEvaluationSheet();
  }

  private loadInitialData(): void {
    const staffId = this.getCurrentStaffId();

    const handleTeacherData = (id?: number | string) => {
      if (!id) {
        this.gradeClassGroups = [];
        this.subjectsByClassId.clear();
        return;
      }

      this.phanCongService.getClassesByStaff(id).subscribe({
        next: ({ data: teacherData }) => {
          const classrooms = (teacherData ??
            []) as TeacherClassAssignmentResponse[];
          this.subjectsByClassId.clear();

          // Map classrooms to dropdown options (be tolerant about field names)
          const classroomOptions = classrooms
            .map((c) => ({
              label: c.className ?? c.classCode ?? c.name ?? '',
              value: this.getClassId(c),
            }))
            .filter(
              (option): option is { label: string; value: string | number } =>
                option.value != null
            );

          // Update the classroom dropdown control
          try {
            this.findFormControl(
              this.$formItem,
              this.key.CLASSROOM_ID
            ).options = classroomOptions;
          } catch {
            const item = (this.$formItem as any[]).find(
              (it: any) => it.key === this.key.CLASSROOM_ID
            );
            if (item) item.options = classroomOptions;
          }

          // If API already returns grouped data, use it directly; otherwise build simple groups
          if (
            Array.isArray(classrooms) &&
            classrooms.length &&
            classrooms[0].classes
          ) {
            this.gradeClassGroups =
              (teacherData as unknown as DiemDanhKhoiNhomItem[]) ?? [];
          } else {
            this.gradeClassGroups =
              this.mapTeacherClassesToGradeGroups(classrooms);
          }

          for (const c of classrooms) {
            const classId = this.getClassId(c);
            if (classId != null && c.subjects?.length) {
              this.subjectsByClassId.set(`${classId}`, c.subjects);
            }
          }

          const firstGroup = this.gradeClassGroups[0];
          const firstClass = firstGroup?.classes?.[0];
          if (firstGroup && firstClass) {
            this.expandedGradeIds = new Set([`${firstGroup.gradeLevelId}`]);
            this.selectClass(firstGroup, firstClass);
          }
        },
        error: () => {
          this.gradeClassGroups = [];
          this.subjectsByClassId.clear();
        },
      });
    };

    // Load current school year (separate concern)
    this.namHocService.getCurrent().subscribe({
      next: ({ data }) => {
        this.currentSchoolYear = data as NamHocOptionResponse | undefined;
        // load semester options for current school year
        const syId = this.currentSchoolYear?.id;
        if (syId != null) {
          this.loadSemesterOptions(syId);
        }
      },
      error: () => {
        this.currentSchoolYear = undefined;
      },
    });

    // Call classes API immediately if staffId available, otherwise wait for auth service
    if (staffId) {
      console.debug(
        'StudentEvaluationBook: staffId from currentUser available',
        staffId
      );
      handleTeacherData(staffId);
    } else {
      console.debug(
        'StudentEvaluationBook: staffId not available yet, waiting for auth.currentUser$'
      );
      this.authService.currentUser$
        .pipe(
          filter((u) => !!u),
          take(1)
        )
        .subscribe((u) => {
          const sid = this.getCurrentStaffId(u);
          console.debug(
            'StudentEvaluationBook: got staffId from auth.currentUser$',
            sid
          );
          handleTeacherData(sid);
        });
    }
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

  selectClass(group: DiemDanhKhoiNhomItem, classroom: DiemDanhLopItem): void {
    this.selectedClassId = classroom.id;
    this.selectedClassName = classroom.name;
    this.expandedGradeIds.add(`${group.gradeLevelId}`);

    // set form control for classroom
    this.form.patchValue(
      { [this.key.CLASSROOM_ID]: classroom.id },
      { emitEvent: false }
    );

    const cachedSubjects = this.subjectsByClassId.get(`${classroom.id}`);
    if (cachedSubjects) {
      this.setSelectedSubjects(cachedSubjects);
      return;
    }

    // load subjects for classroom
    this.lopMonHocService.getDetail(classroom.id).subscribe({
      next: ({ data }) => {
        this.setSelectedSubjects(data?.subjects ?? []);
      },
      error: () => {
        this.setSelectedSubjects([]);
      },
    });
  }

  private getCurrentStaffId(
    user = this.authService.currentUser
  ): string | number | undefined {
    const authUser = user as any;
    return (
      authUser?.staff?.id ??
      authUser?.staffId ??
      authUser?.teacherId ??
      authUser?.profile?.staffId
    );
  }

  private getClassId(
    classroom: TeacherClassAssignmentResponse
  ): string | number | undefined {
    return classroom.classId ?? classroom.classroomId ?? classroom.id;
  }

  private loadSemesterOptions(schoolYearId: ID_TYPE): void {
    this.hocKyService.getOptions(schoolYearId).subscribe({
      next: ({ data }) => {
        const options = (data ?? []).map((item: { id: number; name: string }) => ({
          value: item.id,
          label: item.name,
        }));
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = options;

        if (options.length > 0 && !this.form.get(this.key.SEMESTER_ID)?.value) {
          this.form.patchValue({ [this.key.SEMESTER_ID]: options[0].value }, { emitEvent: false });
          this.loadEvaluationSheet();
        }
      },
      error: () => {
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];
      },
    });
  }
  private mapTeacherClassesToGradeGroups(
    classrooms: TeacherClassAssignmentResponse[]
  ): DiemDanhKhoiNhomItem[] {
    const groups = new Map<string, DiemDanhKhoiNhomItem>();

    for (const classroom of classrooms) {
      const classId = this.getClassId(classroom);
      if (classId == null) continue;

      const gradeNumber =
        classroom.gradeNumber ??
        this.extractGradeNumber(
          classroom.className ?? classroom.classCode ?? classroom.name
        );
      const gradeLevelId =
        classroom.gradeLevelId ?? classroom.gradeId ?? gradeNumber ?? 'unknown';
      const gradeLevelName =
        classroom.gradeLevelName ??
        classroom.gradeName ??
        (gradeNumber ? `Khối ${gradeNumber}` : 'Khối lớp');
      const groupKey = `${gradeLevelId}`;

      if (!groups.has(groupKey)) {
        groups.set(groupKey, {
          gradeLevelId,
          gradeLevelName,
          gradeNumber,
          classes: [],
        });
      }

      groups.get(groupKey)!.classes.push({
        id: classId,
        name:
          classroom.className ?? classroom.name ?? classroom.classCode ?? '',
      });
    }

    return [...groups.values()].sort(
      (a, b) => (a.gradeNumber ?? 999) - (b.gradeNumber ?? 999)
    );
  }

  private extractGradeNumber(value?: string): number | undefined {
    const match = value?.match(/(?:lớp|lop|khối|khoi)?\s*(\d{1,2})/i);
    return match ? Number(match[1]) : undefined;
  }

  private setSelectedSubjects(
    subjects: LopMonHocDetailSubjectResponse[]
  ): void {
    this.subjects = subjects;
    this.selectedSubjectId = subjects.length
      ? subjects[0].subjectId
      : undefined;
    this.loadEvaluationSheet();
  }

  reloadClassGroups(): void {
    this.loadInitialData();
  }

  selectSubject(subjectId: any): void {
    this.selectedSubjectId = subjectId;
    this.loadEvaluationSheet();
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  save() {
    const classroomId = this.selectedClassId;
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;
    const subjectId = this.selectedSubjectId;

    if (!classroomId || !semesterId || !subjectId) {
      this.toastr.warning('Vui lòng chọn đầy đủ Lớp, Học kỳ và Môn học', 'Cảnh báo');
      return;
    }

    const payload: EvaluationBulkSaveRequest = {
      classroomId: Number(classroomId),
      subjectId: Number(subjectId),
      semesterId: Number(semesterId),
      items: this.dataSource.map(s => ({
        studentId: Number(s.id),
        midtermLevel: s.middleTerm,
        midtermRemark: s.commentGK,
        finalLevel: s.finalTerm,
        finalRemark: s.commentFinal,
      })),
    };

    this.evaluationService.saveBulk(payload).subscribe({
      next: () => {
        this.toastr.success('Lưu thành công', 'Thành công');
      },
      error: (err) => {
        this.toastr.error(
          err?.error?.userMessage ?? err?.error?.message ?? 'Lưu thất bại',
          'Lỗi'
        );
      }
    });
  }

  cancel() {
    // Reload the data from the server to discard changes
    this.loadEvaluationSheet();
    this.toastr.info('Đã hủy bỏ các thay đổi', 'Thông báo');
  }

  exportExcel() {
    this.toastr.success('Xuất Excel thành công (demo)', 'Thành công');
  }

  exportPdf() {
    this.toastr.success('Xuất PDF thành công (demo)', 'Thành công');
  }

  openConfig() {
    this.toastr.info('Cấu hình (demo)', 'Thông tin');
  }

  openComment() {
    const classroomId = this.selectedClassId;
    const subjectId = this.selectedSubjectId;
    
    // Lấy thông tin học kỳ đang chọn để xác định hậu tố (1 hoặc 2)
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;
    const semesterOption = (this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options as any[])?.find(o => o.value === semesterId);
    const semesterName = semesterOption?.label || '';
    const semesterSuffix = semesterName.includes('2') ? '2' : '1';

    if (!classroomId || !subjectId) {
      this.toastr.warning('Vui lòng chọn đầy đủ Lớp và Môn học để sinh nhận xét', 'Cảnh báo');
      return;
    }

    const gkItems: EvaluationBulkGenerateCommentItem[] = [];
    const ckItems: EvaluationBulkGenerateCommentItem[] = [];

    for (const row of this.dataSource) {
      if (row.middleTerm && !row.commentGK) {
        gkItems.push({ studentId: Number(row.id), evaluation: row.middleTerm });
      }

      if (row.finalTerm && !row.commentFinal) {
        ckItems.push({ studentId: Number(row.id), evaluation: row.finalTerm });
      }
    }

    if (gkItems.length === 0 && ckItems.length === 0) {
      this.toastr.info('Không có học sinh nào cần sinh nhận xét mới (chưa có điểm T/H/C hoặc đã có nhận xét).', 'Thông báo');
      return;
    }

    this.toastr.info(`Đang sinh nhận xét hàng loạt...`, 'Đang xử lý');

    const observables = [];

    if (gkItems.length > 0) {
      const payload: EvaluationBulkGenerateCommentRequest = {
        classroomId: Number(classroomId),
        subjectId: Number(subjectId),
        term: 'GK' + semesterSuffix,
        items: gkItems
      };
      observables.push(
        this.evaluationService.bulkGenerateComment(payload).pipe(
          map(res => ({ term: 'GK', data: res.data })),
          catchError(err => of({ term: 'GK', error: err }))
        )
      );
    }

    if (ckItems.length > 0) {
      const payload: EvaluationBulkGenerateCommentRequest = {
        classroomId: Number(classroomId),
        subjectId: Number(subjectId),
        term: 'CK' + semesterSuffix,
        items: ckItems
      };
      observables.push(
        this.evaluationService.bulkGenerateComment(payload).pipe(
          map(res => ({ term: 'CK', data: res.data })),
          catchError(err => of({ term: 'CK', error: err }))
        )
      );
    }

    forkJoin(observables).subscribe((results: any[]) => {
      let hasError = false;
      let successCount = 0;

      const gkMap = results.find(r => r.term === 'GK')?.data || {};
      const ckMap = results.find(r => r.term === 'CK')?.data || {};
      
      hasError = results.some(r => !!r.error);

      this.dataSource = this.dataSource.map(row => {
        let updated = false;
        const newRow = { ...row };

        if (gkMap[row.id]) {
          newRow.commentGK = gkMap[row.id];
          updated = true;
          successCount++;
        }

        if (ckMap[row.id]) {
          newRow.commentFinal = ckMap[row.id];
          updated = true;
          successCount++;
        }

        return updated ? newRow : row;
      });

      if (successCount > 0) {
        this.toastr.success(`Đã sinh xong ${successCount} nhận xét`, 'Thành công');
      } else if (!hasError) {
        this.toastr.warning('Không có nhận xét nào được sinh', 'Cảnh báo');
      }

      if (hasError) {
        this.toastr.error('Có lỗi xảy ra trong quá trình sinh nhận xét.', 'Lỗi');
      }
    });
  }

  openImport() {
    this.toastr.info('Nhập dữ liệu (demo)', 'Thông tin');
  }

  /** Load evaluation sheet from API: GET /api/evaluations/sheet */
  private loadEvaluationSheet(): void {
    const classroomId = this.selectedClassId;
    const subjectId = this.selectedSubjectId;
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;

    if (!classroomId || !subjectId || !semesterId) {
      this.dataSource = [];
      this.dataSourceTotal = 0;
      return;
    }

    this.evaluationService.getSheet(classroomId, subjectId, semesterId).subscribe({
      next: ({ data }) => {
        const students = data?.students ?? [];
        this.dataSource = students.map((s: any) => ({
          id: s.studentId,
          fullName: s.studentName ?? '',
          code: s.studentCode ?? '',
          middleTerm: s.midtermLevel ?? '',
          commentGK: s.midtermRemark ?? '',
          finalTerm: s.finalLevel ?? '',
          commentFinal: s.finalRemark ?? '',
        }));
        this.dataSourceTotal = this.dataSource.length;
      },
      error: (error) => {
        this.dataSource = [];
        this.dataSourceTotal = 0;
        this.toastr.error(
          error?.error?.userMessage ??
          error?.error?.message ??
          'Không tải được bảng đánh giá',
          'Thất bại'
        );
      },
    });
  }

  /** Handle inline cell edits – update dataSource in-place */
  onCellChange(row: any, field: string, valueOrEvent: any): void {
    let newValue = valueOrEvent;
    if (valueOrEvent && valueOrEvent.target !== undefined) {
      // It's an Event object (from textarea input)
      newValue = valueOrEvent.target.value;
    }
    
    // Cập nhật row hiện tại trên giao diện
    row[field] = newValue;

    // Đảm bảo cập nhật chính xác vào dataSource gốc (tránh trường hợp table clone data)
    if (this.dataSource) {
      const dataRow = this.dataSource.find(item => item.id === row.id);
      if (dataRow) {
        dataRow[field] = newValue;
      }
    }
  }

  /** Handle quick keys T, H, C */
  onTermKeydown(row: any, field: string, event: KeyboardEvent): void {
    const key = event.key.toUpperCase();
    let val = '';
    if (key === 'T') val = 'T';
    else if (key === 'H') val = 'H';
    else if (key === 'C') val = 'C';

    if (val) {
      event.preventDefault();
      row[field] = val;
      
      if (this.dataSource) {
        const dataRow = this.dataSource.find(item => item.id === row.id);
        if (dataRow) {
          dataRow[field] = val;
        }
      }
    }
  }
}
