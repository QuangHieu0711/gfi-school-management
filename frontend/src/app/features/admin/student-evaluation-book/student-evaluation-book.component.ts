/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { MtxSelectModule } from '@ng-matero/extensions/select';
import { catchError, map } from 'rxjs/operators';
import { filter, forkJoin, of, take } from 'rxjs';

import { LopMonHocDetailSubjectResponse } from '@app/model/admin/lop-mon-hoc.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import {
  DiemDanhKhoiNhomItem,
  DiemDanhLopItem,
} from '@app/model/admin/diem-danh.model';
import {
  EvaluationBulkGenerateCommentItem,
  EvaluationBulkGenerateCommentRequest,
  EvaluationBulkSaveRequest,
  EvaluationEditWindowResponse,
} from '@app/model/admin/evaluation.model';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { LopMonHocService } from '@app/service/admin/lop-mon-hoc.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';
import { EvaluationService } from '@app/service/admin/evaluation.service';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { COMMON_TABLE_KEY, TableConfig } from '@model/table.model';
import { FormType, SELECT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { AuthService, PermissionCheckService } from '@service';

import { DialogEditWindowComponent } from './dialog-edit-window/dialog-edit-window.component';
import { DialogImportEvaluationComponent } from './dialog-import/dialog-import-evaluation.component';

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
  readonly menuCode = 'STUDENT_EVALUATION_BOOK';

  @ViewChild('nameTpl', { static: true }) nameTpl!: TemplateRef<unknown>;
  @ViewChild('middleTermTpl', { static: true })
  middleTermTpl!: TemplateRef<unknown>;
  @ViewChild('commentGKTpl', { static: true })
  commentGKTpl!: TemplateRef<unknown>;
  @ViewChild('finalTermScoreTpl', { static: true })
  finalTermScoreTpl!: TemplateRef<unknown>;
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

  readonly $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: this.key.SEMESTER_ID,
      placeholder: 'Chọn học kỳ',
      required: false,
      listOption: [],
      clearable: true,
    }),
  ];

  readonly tableConfig: TableConfig = {
    hasFilterPanel: true,
    hasFilterPanelButton: false,
    hasExport: false,
    showPaginator: false,
  };

  columns: MtxGridColumn[] = [];
  dataSource: any[] = [];
  editWindowConfig: EvaluationEditWindowResponse | null = null;
  editWindowMessage = 'Chưa cấu hình thời gian sửa điểm cho học kỳ này.';

  gradeClassGroups: DiemDanhKhoiNhomItem[] = [];
  expandedGradeIds = new Set<string>();
  selectedClassId?: string | number;
  selectedClassName = '';
  subjects: LopMonHocDetailSubjectResponse[] = [];
  selectedSubjectId?: string | number;
  currentSchoolYear?: NamHocOptionResponse;
  classroomOptions: Array<{ label: string; value: string | number }> = [];

  private readonly scoredSubjectAliases = [
    'toan',
    'tieng viet',
    'khoa hoc',
    'lich su va dia li',
    'lich su & dia li',
    'tieng anh',
  ];

  private subjectsByClassId = new Map<
    string,
    LopMonHocDetailSubjectResponse[]
  >();

  get canConfig(): boolean {
    return this.permissionCheckService.canConfig(this.menuCode);
  }

  get canEditBook(): boolean {
    return this.permissionCheckService.canEdit(this.menuCode);
  }

  get isEditWindowOpen(): boolean {
    if (!this.canEditBook) return false;

    const startDate = this.normalizeDateValue(this.editWindowConfig?.startDate);
    const endDate = this.normalizeDateValue(this.editWindowConfig?.endDate);
    if (!startDate || !endDate) return false;

    const today = this.getTodayKey();
    return today >= startDate && today <= endDate;
  }

  get selectedSubjectName(): string {
    if (!this.selectedSubjectId || !this.subjects.length) return '';
    const found = this.subjects.find(
      (subject) => subject.subjectId === this.selectedSubjectId
    );
    return found?.subjectName ?? '';
  }

  constructor(
    protected override injector: Injector,
    private readonly phanCongService: PhanCongGiangDayService,
    private readonly lopMonHocService: LopMonHocService,
    private readonly namHocService: NamHocService,
    private readonly hocKyService: HocKyService,
    private readonly authService: AuthService,
    private readonly evaluationService: EvaluationService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.dataSource = [];
    this.dataSourceTotal = 0;
    this.updateColumns();
    this.loadInitialData();
  }

  filterData(pageChangeEvent?: any): void {
    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;
    this.loadEvaluationSheet();
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

    this.form.patchValue(
      { [this.key.CLASSROOM_ID]: classroom.id },
      { emitEvent: false }
    );

    const cachedSubjects = this.subjectsByClassId.get(`${classroom.id}`);
    if (cachedSubjects) {
      this.setSelectedSubjects(cachedSubjects);
      return;
    }

    this.lopMonHocService.getDetail(classroom.id).subscribe({
      next: ({ data }) => {
        this.setSelectedSubjects(data?.subjects ?? []);
      },
      error: () => {
        this.setSelectedSubjects([]);
      },
    });
  }

  selectSubject(subjectId: any): void {
    this.selectedSubjectId = subjectId;
    this.updateColumns();
    this.loadEvaluationSheet();
  }

  reloadClassGroups(): void {
    this.loadInitialData();
  }

  resetFilter(): void {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  save(): void {
    if (!this.isEditWindowOpen) {
      this.toastr.warning(this.editWindowMessage, 'Cảnh báo');
      return;
    }

    const classroomId = this.selectedClassId;
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;
    const subjectId = this.selectedSubjectId;

    if (!classroomId || !semesterId || !subjectId) {
      this.toastr.warning(
        'Vui lòng chọn đầy đủ Lớp, Học kỳ và Môn học',
        'Cảnh báo'
      );
      return;
    }

    const payload: EvaluationBulkSaveRequest = {
      classroomId: Number(classroomId),
      subjectId: Number(subjectId),
      semesterId: Number(semesterId),
      items: this.dataSource.map((student) => ({
        studentId: Number(student.id),
        midtermLevel: student.middleTerm,
        midtermScore: student.middleTermScore,
        midtermRemark: student.commentGK,
        finalLevel: student.finalTerm,
        finalScore: student.finalTermScore,
        finalRemark: student.commentFinal,
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
      },
    });
  }

  cancel(): void {
    this.loadEvaluationSheet();
    this.toastr.info('Đã hủy bỏ các thay đổi', 'Thông báo');
  }

  exportExcel(): void {
    this.toastr.success('Xuất Excel thành công (demo)', 'Thành công');
  }

  exportPdf(): void {
    this.toastr.success('Xuất PDF thành công (demo)', 'Thành công');
  }

  openConfig(): void {
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;
    if (!semesterId) {
      this.toastr.warning(
        'Vui lòng chọn học kỳ để cấu hình thời gian sửa điểm',
        'Cảnh báo'
      );
      return;
    }

    const semesterOptions = this.findFormControl(
      this.$formItem,
      this.key.SEMESTER_ID
    ).options as any[];
    const semesterOption = semesterOptions?.find(
      (option) => option.value === semesterId
    );

    this.dialog.componentDialog(
      DialogEditWindowComponent,
      {
        width: '480px',
        data: {
          semesterId: Number(semesterId),
          semesterName: semesterOption?.label || '',
          currentConfig: this.editWindowConfig,
        },
      },
      (result?: boolean) => {
        if (result) {
          this.loadEditWindowConfig();
        }
      }
    );
  }

  openComment(): void {
    if (!this.isEditWindowOpen) {
      this.toastr.warning(this.editWindowMessage, 'Cảnh báo');
      return;
    }

    const classroomId = this.selectedClassId;
    const subjectId = this.selectedSubjectId;
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;
    const semesterOption = (
      this.findFormControl(this.$formItem, this.key.SEMESTER_ID)
        .options as any[]
    )?.find((option) => option.value === semesterId);
    const semesterName = semesterOption?.label || '';
    const semesterSuffix = semesterName.includes('2') ? '2' : '1';

    if (!classroomId || !subjectId) {
      this.toastr.warning(
        'Vui lòng chọn đầy đủ Lớp và Môn học để sinh nhận xét',
        'Cảnh báo'
      );
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
      this.toastr.info(
        'Không có học sinh nào cần sinh nhận xét mới (chưa có điểm T/H/C hoặc đã có nhận xét).',
        'Thông báo'
      );
      return;
    }

    this.toastr.info('Đang sinh nhận xét hàng loạt...', 'Đang xử lý');

    const requests = [];

    if (gkItems.length > 0) {
      const payload: EvaluationBulkGenerateCommentRequest = {
        classroomId: Number(classroomId),
        subjectId: Number(subjectId),
        term: 'GK' + semesterSuffix,
        items: gkItems,
      };
      requests.push(
        this.evaluationService.bulkGenerateComment(payload).pipe(
          map((response) => ({ term: 'GK', data: response.data })),
          catchError((error) => of({ term: 'GK', error }))
        )
      );
    }

    if (ckItems.length > 0) {
      const payload: EvaluationBulkGenerateCommentRequest = {
        classroomId: Number(classroomId),
        subjectId: Number(subjectId),
        term: 'CK' + semesterSuffix,
        items: ckItems,
      };
      requests.push(
        this.evaluationService.bulkGenerateComment(payload).pipe(
          map((response) => ({ term: 'CK', data: response.data })),
          catchError((error) => of({ term: 'CK', error }))
        )
      );
    }

    forkJoin(requests).subscribe((results: any[]) => {
      const gkMap = results.find((item) => item.term === 'GK')?.data || {};
      const ckMap = results.find((item) => item.term === 'CK')?.data || {};
      const hasError = results.some((item) => !!item.error);
      let successCount = 0;

      this.dataSource = this.dataSource.map((row) => {
        let updated = false;
        const nextRow = { ...row };

        if (gkMap[row.id]) {
          nextRow.commentGK = gkMap[row.id];
          updated = true;
          successCount++;
        }

        if (ckMap[row.id]) {
          nextRow.commentFinal = ckMap[row.id];
          updated = true;
          successCount++;
        }

        return updated ? nextRow : row;
      });

      if (successCount > 0) {
        this.toastr.success(
          `Đã sinh xong ${successCount} nhận xét`,
          'Thành công'
        );
      } else if (!hasError) {
        this.toastr.warning('Không có nhận xét nào được sinh', 'Cảnh báo');
      }

      if (hasError) {
        this.toastr.error(
          'Có lỗi xảy ra trong quá trình sinh nhận xét.',
          'Lỗi'
        );
      }
    });
  }

  openImport(): void {
    if (!this.isEditWindowOpen) {
      this.toastr.warning(this.editWindowMessage, 'Cảnh báo');
      return;
    }

    const classroomId = this.selectedClassId;
    const subjectId = this.selectedSubjectId;
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;

    if (!classroomId || !subjectId || !semesterId) {
      this.toastr.warning(
        'Vui lòng chọn đầy đủ Lớp, Môn học và Học kỳ để kết nạp',
        'Cảnh báo'
      );
      return;
    }

    const semesterOptions = this.findFormControl(
      this.$formItem,
      this.key.SEMESTER_ID
    ).options as any[];
    const semesterOption = semesterOptions?.find(
      (option) => option.value === semesterId
    );

    this.dialog.componentDialog(
      DialogImportEvaluationComponent,
      {
        width: '900px',
        data: {
          classroomId: Number(classroomId),
          subjectId: Number(subjectId),
          semesterId: Number(semesterId),
          classroomName: this.selectedClassName,
          subjectName: this.selectedSubjectName,
          semesterName: semesterOption?.label || '',
          classroomOptions: this.classroomOptions,
          semesterOptions,
          subjectsByClassId: this.subjectsByClassId,
        },
      },
      (result: any) => {
        if (result) {
          this.loadEvaluationSheet();
        }
      }
    );
  }

  onCellChange(row: any, field: string, valueOrEvent: any): void {
    if (!this.isEditWindowOpen) return;

    let newValue = valueOrEvent;
    if (valueOrEvent && valueOrEvent.target !== undefined) {
      newValue = valueOrEvent.target.value;
    }

    row[field] = newValue;

    const dataRow = this.dataSource.find((item) => item.id === row.id);
    if (dataRow) {
      dataRow[field] = newValue;
    }
  }

  onTermKeydown(row: any, field: string, event: KeyboardEvent): void {
    if (!this.isEditWindowOpen) return;

    const key = event.key.toUpperCase();
    let value = '';

    if (key === 'T') value = 'T';
    else if (key === 'H') value = 'H';
    else if (key === 'C') value = 'C';

    if (!value) return;

    event.preventDefault();
    row[field] = value;

    const dataRow = this.dataSource.find((item) => item.id === row.id);
    if (dataRow) {
      dataRow[field] = value;
    }
  }

  onScoreChange(row: any, field: string, score: any): void {
    if (!this.isEditWindowOpen) return;

    const numericScore =
      score !== null && score !== '' ? parseFloat(score) : null;
    row[field] = numericScore;

    if (numericScore !== null && !isNaN(numericScore)) {
      const levelField =
        field === 'middleTermScore' ? 'middleTerm' : 'finalTerm';
      row[levelField] = this.calculateLevelFromScore(numericScore);
    }

    const dataRow = this.dataSource.find((item) => item.id === row.id);
    if (dataRow) {
      dataRow[field] = numericScore;
      if (numericScore !== null && !isNaN(numericScore)) {
        const levelField =
          field === 'middleTermScore' ? 'middleTerm' : 'finalTerm';
        dataRow[levelField] = this.calculateLevelFromScore(numericScore);
      }
    }
  }

  private updateColumns(): void {
    const currentSubject = this.subjects.find(
      (subject) => subject.subjectId == this.selectedSubjectId
    );
    const normalizedSubjectName = this.normalizeVietnamese(
      currentSubject?.subjectName
    );
    const isScored =
      currentSubject?.subjectType == 1 ||
      this.scoredSubjectAliases.some((name) =>
        normalizedSubjectName.includes(name)
      );

    const columns: MtxGridColumn[] = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
        width: '50px',
      },
      {
        header: 'Họ và tên',
        field: 'fullName',
        width: '210px',
        cellTemplate: this.nameTpl,
      },
      {
        header: 'Mức GK',
        field: 'middleTerm',
        width: '65px',
        class: 'text-center',
        cellTemplate: this.middleTermTpl,
      },
      {
        header: 'Nhận xét GK',
        field: 'commentGK',
        width: '360px',
        cellTemplate: this.commentGKTpl,
      },
    ];

    if (isScored) {
      columns.push({
        header: 'Điểm CK',
        field: 'finalTermScore',
        width: '80px',
        class: 'text-center',
        cellTemplate: this.finalTermScoreTpl,
      });
    }

    columns.push(
      {
        header: 'Mức CK',
        field: 'finalTerm',
        width: '65px',
        class: 'text-center',
        cellTemplate: this.finalTermTpl,
      },
      {
        header: 'Nhận xét',
        field: 'commentFinal',
        width: '360px',
        cellTemplate: this.commentFinalTpl,
      }
    );

    this.columns = columns;
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

          this.classroomOptions = classrooms
            .map((classroom) => ({
              label:
                classroom.className ?? classroom.classCode ?? classroom.name ?? '',
              value: this.getClassId(classroom),
            }))
            .filter(
              (
                option
              ): option is { label: string; value: string | number } =>
                option.value != null
            );

          try {
            this.findFormControl(
              this.$formItem,
              this.key.CLASSROOM_ID
            ).options = this.classroomOptions;
          } catch {
            const item = (this.$formItem as any[]).find(
              (it: any) => it.key === this.key.CLASSROOM_ID
            );
            if (item) item.options = this.classroomOptions;
          }

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

          for (const classroom of classrooms) {
            const classId = this.getClassId(classroom);
            if (classId != null && classroom.subjects?.length) {
              this.subjectsByClassId.set(`${classId}`, classroom.subjects);
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

    this.namHocService.getCurrent().subscribe({
      next: ({ data }) => {
        this.currentSchoolYear = data as NamHocOptionResponse | undefined;
        const schoolYearId = this.currentSchoolYear?.id;
        if (schoolYearId != null) {
          this.loadSemesterOptions(schoolYearId);
        }
      },
      error: () => {
        this.currentSchoolYear = undefined;
      },
    });

    if (staffId) {
      handleTeacherData(staffId);
      return;
    }

    this.authService.currentUser$
      .pipe(
        filter((user) => !!user),
        take(1)
      )
      .subscribe((user) => {
        handleTeacherData(this.getCurrentStaffId(user));
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
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options =
          options;

        if (options.length > 0 && !this.form.get(this.key.SEMESTER_ID)?.value) {
          this.form.patchValue(
            { [this.key.SEMESTER_ID]: options[0].value },
            { emitEvent: false }
          );
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
    this.selectedSubjectId = subjects.length ? subjects[0].subjectId : undefined;
    this.updateColumns();
    this.loadEvaluationSheet();
  }

  private loadEvaluationSheet(): void {
    const classroomId = this.selectedClassId;
    const subjectId = this.selectedSubjectId;
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;

    this.loadEditWindowConfig();

    if (!classroomId || !subjectId || !semesterId) {
      this.dataSource = [];
      this.dataSourceTotal = 0;
      return;
    }

    this.updateColumns();
    this.evaluationService.getSheet(classroomId, subjectId, semesterId).subscribe({
      next: ({ data }) => {
        const students = data?.students ?? [];
        this.dataSource = students.map((student: any) => ({
          id: student.studentId,
          fullName: student.studentName ?? '',
          code: student.studentCode ?? '',
          middleTerm: student.midtermLevel ?? '',
          middleTermScore: student.midtermScore,
          commentGK: student.midtermRemark ?? '',
          finalTerm: student.finalLevel ?? '',
          finalTermScore: student.finalScore,
          commentFinal: student.finalRemark ?? '',
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

  private calculateLevelFromScore(score: number): string {
    if (score >= 9) return 'T';
    if (score >= 5) return 'H';
    return 'C';
  }

  private loadEditWindowConfig(): void {
    const semesterId = this.form.get(this.key.SEMESTER_ID)?.value;

    if (!semesterId) {
      this.editWindowConfig = null;
      this.editWindowMessage =
        'Vui lòng chọn học kỳ để kiểm tra thời gian sửa điểm.';
      return;
    }

    this.evaluationService.getEditWindow(semesterId).subscribe({
      next: ({ data }) => {
        this.editWindowConfig = data ?? null;
        this.editWindowMessage = this.buildEditWindowMessage();
      },
      error: () => {
        this.editWindowConfig = null;
        this.editWindowMessage =
          'Chưa cấu hình thời gian sửa điểm cho học kỳ này.';
      },
    });
  }

  private buildEditWindowMessage(): string {
    if (!this.canEditBook) {
      return 'Bạn không có quyền chỉnh sửa sổ đánh giá.';
    }

    const startDate = this.normalizeDateValue(this.editWindowConfig?.startDate);
    const endDate = this.normalizeDateValue(this.editWindowConfig?.endDate);

    if (!startDate || !endDate) {
      return 'Chưa cấu hình thời gian sửa điểm cho học kỳ này.';
    }

    const rangeText = `${this.formatDate(startDate)} - ${this.formatDate(endDate)}`;
    return this.isEditWindowOpen
      ? `Đang trong thời gian được phép sửa điểm: ${rangeText}.`
      : `Ngoài thời gian sửa điểm. Thời gian được phép: ${rangeText}.`;
  }

  private normalizeDateValue(value?: string | null): string {
    if (!value) return '';
    return value.slice(0, 10);
  }

  private getTodayKey(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = `${now.getMonth() + 1}`.padStart(2, '0');
    const day = `${now.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDate(value?: string | null): string {
    const raw = this.normalizeDateValue(value);
    if (!raw) return '--';
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  private normalizeVietnamese(value?: string | null): string {
    return (value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .replace(/Đ/g, 'D')
      .toLowerCase()
      .trim();
  }
}
