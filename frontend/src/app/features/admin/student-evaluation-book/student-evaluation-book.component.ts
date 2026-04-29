/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { filter, take } from 'rxjs';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { FormType, SELECT_CONTROL } from '@model/form-control.model';
import { ComponentBaseAbstract } from '@layout';
import { COMMON_TABLE_KEY } from '@model/table.model';
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
import { AuthService } from '@service';

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
      controlName: this.key.SEMESTER_ID,
      label: 'Học kỳ',
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
  selectedClassId?: string | number;
  selectedClassName = '';
  subjects: LopMonHocDetailSubjectResponse[] = [];
  selectedSubjectId?: any;
  currentSchoolYear?: NamHocOptionResponse;
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
