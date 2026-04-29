/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { FormType, SELECT_CONTROL } from '@model/form-control.model';
import { ComponentBaseAbstract } from '@layout';
import { COMMON_TABLE_KEY } from '@model/table.model';
import { DiemDanhKhoiNhomItem, DiemDanhLopItem } from '@app/model/admin/diem-danh.model';
import { DiemDanhService } from '@app/service/admin/diem-danh.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';
import { LopMonHocDetailSubjectResponse } from '@app/model/admin/lop-mon-hoc.model';
import { LopMonHocService } from '@app/service/admin/lop-mon-hoc.service';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { AuthService } from '@service';

@Component({
  selector: 'student-evaluation-book',
  standalone: true,
  templateUrl: './student-evaluation-book.component.html',
  styleUrls: ['./student-evaluation-book.component.scss'],
  imports: [
    CommonModule,
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class StudentEvaluationBookComponent extends ComponentBaseAbstract {
  @ViewChild('nameTpl', { static: true }) nameTpl!: TemplateRef<unknown>;

  readonly key = {
    CLASSROOM_ID: 'classroomId',
    SEMESTER_ID: 'semesterId',
    INPUT_ROUND: 'inputRound',
  } as const;

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: this.key.CLASSROOM_ID,
      label: 'Lớp',
      required: false,
      listOption: [],
      clearable: true,
    }),
    SELECT_CONTROL({
      controlName: this.key.SEMESTER_ID,
      label: 'Học kỳ',
      required: false,
      listOption: [],
      clearable: true,
    }),
    SELECT_CONTROL({
      controlName: this.key.INPUT_ROUND,
      label: 'Đợt nhập điểm',
      required: false,
      listOption: [],
      clearable: true,
    }),
  ];

  tableConfig = {
    hasFilterPanel: true,
    hasFilterPanelButton: false,
    hasExport: true,
  };
  columns: MtxGridColumn[] = [];
  dataSource: any[] = [];

  // Class picker / subject UI state
  gradeClassGroups: DiemDanhKhoiNhomItem[] = [];
  expandedGradeIds = new Set<string>();
  showClassPicker = false;
  selectedClassId?: string | number;
  selectedClassName = '';
  subjects: LopMonHocDetailSubjectResponse[] = [];
  selectedSubjectId?: any;
  teacherAssignedClassIds = new Set<string>();
  currentSchoolYear?: NamHocOptionResponse;

  constructor(
    protected override injector: Injector,
    private readonly diemDanhService: DiemDanhService,
    private readonly phanCongService: PhanCongGiangDayService,
    private readonly lopMonHocService: LopMonHocService,
    private readonly namHocService: NamHocService,
    private readonly authService: AuthService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    // Initialize form controls and load class/subject data
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.loadInitialData();

    // Sample students
    this.dataSource = [
      {
        id: 1,
        fullName: 'Siu Trâm Anh',
        dob: '05/02/2019',
        code: '64632517-00-2469',
        middleTerm: 'H',
        commentGK: 'Hoàn thành nội dung môn học.',
        finalTerm: '',
        commentFinal: '',
      },
      {
        id: 2,
        fullName: 'Ro Lan Chanh',
        dob: '30/04/2019',
        code: '64632517-00-2471',
        middleTerm: 'H',
        commentGK: 'Hoàn thành các yêu cầu học tập của môn học.',
        finalTerm: '',
        commentFinal: '',
      },
      {
        id: 3,
        fullName: 'Nguyễn Ngọc Châu',
        dob: '28/04/2019',
        code: '64632517-00-2463',
        middleTerm: 'H',
        commentGK: 'Có tiến bộ trong học tập.',
        finalTerm: '',
        commentFinal: '',
      },
    ];

    this.dataSourceTotal = this.dataSource.length;

    this.columns = [
      { header: 'STT', class: 'text-center', field: COMMON_TABLE_KEY.STT },
      {
        header: 'Họ và tên',
        field: 'fullName',
        minWidth: 220,
        cellTemplate: this.nameTpl,
      },
      { header: 'Giữa học kỳ', field: 'middleTerm', class: 'text-center' },
      { header: 'Nhận xét GK', field: 'commentGK' },
      { header: 'Cuối học kỳ', field: 'finalTerm', class: 'text-center' },
      { header: 'Nhận xét', field: 'commentFinal' },
    ];
  }

  filterData(pageChangeEvent?: any) {
    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;
    // In a real implementation call API with filter values from `this.form`
    this.dataSourceTotal = this.dataSource.length;
  }

  private loadInitialData(): void {
    const unitId = this.authService.currentUser?.unit?.id;
    const staffId = this.authService.currentUser?.id;

    this.namHocService.getCurrent().subscribe({
      next: ({ data }) => {
        this.currentSchoolYear = data as NamHocOptionResponse | undefined;
        const schoolYearId = data?.id;

        const gradeGroups$ = this.diemDanhService.getGradeClassGroups(
          unitId ?? '',
          schoolYearId ?? ''
        );

        const teacherClasses$ = staffId
          ? this.phanCongService.getClassesByStaff(staffId)
          : of({ data: [] });

        forkJoin([gradeGroups$, teacherClasses$]).subscribe({
          next: ([gradeRes, teacherRes]) => {
            const groups: DiemDanhKhoiNhomItem[] = gradeRes?.data ?? [];
            const teacherList: any[] = teacherRes?.data ?? [];

            // build set of assigned classroom ids (support both {classId} and {id})
            this.teacherAssignedClassIds = new Set(
              teacherList.map((c) => `${c.classId ?? c.id ?? c}`)
            );

            // filter grade groups to only include classes assigned to teacher
            this.gradeClassGroups = (groups ?? [])
              .map((g) => ({
                ...g,
                classes: (g.classes ?? []).filter((cl) =>
                  this.teacherAssignedClassIds.has(`${cl.id}`)
                ),
              }))
              .filter((g) => (g.classes ?? []).length > 0);
          },
          error: () => {
            this.gradeClassGroups = [];
            this.teacherAssignedClassIds = new Set();
          },
        });
      },
      error: () => {
        this.gradeClassGroups = [];
      },
    });
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
    this.showClassPicker = false;

    // set form control for classroom
    this.form.patchValue({ [this.key.CLASSROOM_ID]: classroom.id }, { emitEvent: false });

    // load subjects for classroom
    this.lopMonHocService.getDetail(classroom.id).subscribe({
      next: ({ data }) => {
        this.subjects = (data?.subjects ?? []) as LopMonHocDetailSubjectResponse[];
        this.selectedSubjectId = this.subjects.length
          ? this.subjects[0].subjectId
          : undefined;
      },
      error: () => {
        this.subjects = [];
        this.selectedSubjectId = undefined;
      },
    });
  }

  reloadClassGroups(): void {
    this.loadInitialData();
  }

  selectSubject(subjectId: any): void {
    this.selectedSubjectId = subjectId;
    // optional: set form value or trigger reload of table data filtered by subject
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  save() {
    this.toastr.success('Lưu thành công', 'Thành công');
  }

  exportExcel() {
    this.toastr.success('Xuất Excel thành công (demo)', 'Thành công');
  }

  openConfig() {
    this.toastr.info('Cấu hình (demo)', 'Thông tin');
  }

  openComment() {
    this.toastr.info('Nhận xét (demo)', 'Thông tin');
  }

  openImport() {
    this.toastr.info('Nhập dữ liệu (demo)', 'Thông tin');
  }
}
