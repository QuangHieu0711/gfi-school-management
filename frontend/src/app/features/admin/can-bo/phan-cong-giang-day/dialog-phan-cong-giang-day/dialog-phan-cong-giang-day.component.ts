import { CommonModule } from '@angular/common';
import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { distinctUntilChanged, forkJoin, takeUntil } from 'rxjs';

import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { FormType, IOptions } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { AuthService } from '@service';

import { PhanCongGiaoVienClassroomResponse } from '@app/model/admin/phan-cong-giao-vien.model';
import {
  PhanCongGiangDayDetailRequest,
  PhanCongGiangDayDetailResponse,
  PHAN_CONG_GIANG_DAY_FORM,
  PHAN_CONG_GIANG_DAY_KEY,
  PhanCongGiangDayResponse,
  PhanCongGiangDayUpsertRequest,
} from '@app/model/admin/phan-cong-giang-day.model';
import {
  CanBoDetailResponse,
  CanBoResponse,
} from '@app/model/admin/can-bo.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { MonHocResponse } from '@app/model/admin/mon-hoc.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import { HocKyResponse } from '@app/model/admin/hoc-ky.model';
import { CanBoService } from '@app/service/admin/can-bo.service';
import { KhoiService } from '@app/service/admin/khoi.service';
import { LopService } from '@app/service/admin/lop.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { PhanCongGiaoVienService } from '@app/service/admin/phan-cong-giao-vien.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';

interface ClassOption {
  label: string;
  value: ID_TYPE;
  gradeLevelId?: ID_TYPE;
}

interface StaffGroup {
  gradeId: ID_TYPE;
  gradeName: string;
  staffs: CanBoResponse[];
  expanded: boolean;
}

@Component({
  selector: 'dialog-phan-cong-giang-day',
  standalone: true,
  templateUrl: './dialog-phan-cong-giang-day.component.html',
  styleUrls: ['./dialog-phan-cong-giang-day.component.scss'],
  imports: [
    CommonModule,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
    AppDialogComponent,
  ],
})
export class DialogPhanCongGiangDayComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly key = PHAN_CONG_GIANG_DAY_KEY;
  title = '';
  $formItem: FormType[] = structuredClone(
    PHAN_CONG_GIANG_DAY_FORM
  ) as FormType[];
  classOptions: ClassOption[] = [];
  allClassOptions: ClassOption[] = [];
  staffGroups: StaffGroup[] = [];
  selectedStaff?: CanBoDetailResponse | CanBoResponse;
  selectedStaffGradeId?: ID_TYPE;
  private previousSubjectId?: ID_TYPE;
  private readonly subjectClassSelections = new Map<string, ID_TYPE[]>();
  private readonly subjectClassLabelMap = new Map<
    string,
    Map<string, string>
  >();
  private readonly subjectLabelMap = new Map<string, string>();
  private subjectOrder: string[] = [];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogPhanCongGiangDayComponent>,
    private readonly phanCongService: PhanCongGiangDayService,
    private readonly namHocService: NamHocService,
    private readonly hocKyService: HocKyService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    private readonly canBoService: CanBoService,
    private readonly khoiService: KhoiService,
    private readonly phanCongGiaoVienService: PhanCongGiaoVienService,
    private readonly authService: AuthService,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      type: TYPE_FORM_KEY;
      staffId?: ID_TYPE;
      id?: ID_TYPE;
      data?: PhanCongGiangDayResponse;
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa phân công giảng dạy';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết phân công giảng dạy';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới phân công giảng dạy';
        break;
    }

    this.loadOptions();
    this.bindFormChanges();
    this.previousSubjectId = this.form.get(this.key.SUBJECT_ID)?.value;

    if (this.data.type !== this.TYPE_FORM.CREATE) {
      if (this.data.data) {
        this.patchData(this.data.data);
      }
      this.loadAssignmentDetail();
    }
  }

  onSubmit(): void {
    const payload = this.buildUpsertPayload();

    if (!payload.staffId) {
      this.toastr.error('Vui lòng chọn cán bộ', 'Thất bại');
      return;
    }

    if (
      !payload.assignments.length ||
      payload.assignments.every((item) => !item.classIds.length)
    ) {
      this.toastr.error('Vui lòng chọn lớp phân công', 'Thất bại');
      return;
    }

    // Use the POST endpoint for both create and update (upsert).
    this.phanCongService.create(payload).subscribe({
      next: () => {
        const successMsg =
          this.data.type === this.TYPE_FORM.CREATE
            ? 'Lưu thành công'
            : 'Cập nhật thành công';
        this.toastr.success(successMsg, 'Thành công');
        this.dialogRef.close(true);
      },
      error: (error) => {
        const failMsg =
          error?.error?.userMessage ??
          error?.error?.message ??
          (this.data.type === this.TYPE_FORM.CREATE
            ? 'Lưu thất bại'
            : 'Cập nhật thất bại');
        this.toastr.error(failMsg, 'Thất bại');
      },
    });
  }

  private loadAssignmentDetail(): void {
    const seedData = this.data.data;
    const detailPayload = this.buildDetailPayload(seedData);

    if (!detailPayload) {
      return;
    }

    this.phanCongService.getDetail(detailPayload).subscribe({
      next: ({ data }) => {
        this.patchData({
          ...(seedData ?? {}),
          ...data,
        });
      },
      error: () => {
        if (seedData) {
          this.patchData(seedData);
        }
      },
    });
  }

  private buildDetailPayload(
    seedData?: PhanCongGiangDayResponse
  ): PhanCongGiangDayDetailRequest | undefined {
    const staffId = seedData?.staffId ?? this.data.staffId;
    const schoolYearId = seedData?.schoolYearId;
    const semesterId = seedData?.semesterId;
    const subjectId = seedData?.subjectId;
    const unitId = seedData?.unitId ?? this.getCurrentUnitId();

    if (
      staffId === null ||
      staffId === undefined ||
      staffId === '' ||
      schoolYearId === null ||
      schoolYearId === undefined ||
      schoolYearId === '' ||
      unitId === null ||
      unitId === undefined ||
      unitId === ''
    ) {
      return undefined;
    }

    return {
      unitId,
      staffId,
      schoolYearId,
      semesterId,
      subjectId,
    };
  }

  switchUpdate(): void {
    this.form.enable();
    this.form.get(this.key.ASSIGNMENT_SUMMARY)?.disable({ emitEvent: false });
    this.title = 'Chỉnh sửa phân công giảng dạy';
    this.data.type = this.TYPE_FORM.UPDATE;
    const subjectKey = this.getCurrentSubjectKey();
    if (subjectKey) {
      this.form
        .get(this.key.CLASS_ID)
        ?.setValue(this.subjectClassSelections.get(subjectKey) ?? [], {
          emitEvent: false,
        });
    }
    this.updateAssignmentSummary();
  }

  deleteAssignment(): void {
    if (!this.data.id) return;
    this.dialog.confirm({ message: 'Bạn có chắc chắn muốn xóa phân công này không?' }, (confirmed) => {
      if (confirmed) {
        this.phanCongService.delete(this.data.id!).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.dialogRef.close(true);
          },
          error: (error) => {
            const failMsg = error?.error?.userMessage ?? error?.error?.message ?? 'Xóa thất bại';
            this.toastr.error(failMsg, 'Thất bại');
          }
        });
      }
    });
  }

  toggleGroup(group: StaffGroup): void {
    group.expanded = !group.expanded;
  }

  selectStaff(staff: CanBoResponse, gradeId?: ID_TYPE): void {
    this.selectedStaff = staff;
    this.selectedStaffGradeId =
      gradeId ?? (staff as CanBoDetailResponse).gradeId ?? undefined;
    this.reloadClassOptionsBySubject();
  }

  isSelectedStaff(staffId?: ID_TYPE): boolean {
    return `${this.selectedStaff?.id ?? ''}` === `${staffId ?? ''}`;
  }

  isClassSelected(classId: ID_TYPE): boolean {
    return this.getSelectedClassIds().some(
      (item) => `${item}` === `${classId}`
    );
  }

  selectClass(classId: ID_TYPE, checked?: boolean): void {
    if (this.data.type === this.TYPE_FORM.DETAIL) return;

    const currentClassIds = this.getSelectedClassIds();
    const nextChecked = checked ?? !this.isClassSelected(classId);
    const nextClassIds = nextChecked
      ? [...currentClassIds, classId]
      : currentClassIds.filter((item) => `${item}` !== `${classId}`);

    this.form
      .get(this.key.CLASS_ID)
      ?.setValue(
        this.isCreateMode() ? nextClassIds : (nextClassIds[0] ?? null)
      );
    this.form.get(this.key.CLASS_ID)?.markAsTouched();
    this.syncCurrentSubjectSelection(nextClassIds);
    this.updateAssignmentSummary();
  }

  isAllDisplayedSelected(): boolean {
    const displayedIds = this.getDisplayedClassOptions().map(
      (item) => `${item.value}`
    );
    if (!displayedIds.length) return false;

    const selectedIds = new Set(
      this.getSelectedClassIds().map((item) => `${item}`)
    );
    return displayedIds.every((item) => selectedIds.has(item));
  }

  isSomeDisplayedSelected(): boolean {
    const displayedIds = this.getDisplayedClassOptions().map(
      (item) => `${item.value}`
    );
    if (!displayedIds.length) return false;

    const selectedIds = new Set(
      this.getSelectedClassIds().map((item) => `${item}`)
    );
    const selectedCount = displayedIds.filter((item) =>
      selectedIds.has(item)
    ).length;
    return selectedCount > 0 && selectedCount < displayedIds.length;
  }

  toggleSelectAllClasses(checked: boolean): void {
    if (this.data.type === this.TYPE_FORM.DETAIL) return;

    const displayedClassIds = this.getDisplayedClassOptions().map(
      (item) => item.value
    );
    if (!displayedClassIds.length) return;

    const selectedIds = this.getSelectedClassIds();
    const nextClassIds = checked
      ? Array.from(new Set([...selectedIds, ...displayedClassIds]))
      : selectedIds.filter(
          (item) =>
            !displayedClassIds.some((displayed) => `${displayed}` === `${item}`)
        );

    this.form
      .get(this.key.CLASS_ID)
      ?.setValue(
        this.isCreateMode() ? nextClassIds : (nextClassIds[0] ?? null)
      );
    this.form.get(this.key.CLASS_ID)?.markAsTouched();
    this.syncCurrentSubjectSelection(nextClassIds);
    this.updateAssignmentSummary();
  }

  getDisplayedClassOptions(): ClassOption[] {
    const subjectId = this.form.get(this.key.SUBJECT_ID)?.value;
    if (subjectId !== null && subjectId !== undefined && subjectId !== '') {
      return this.classOptions;
    }

    if (!this.selectedStaffGradeId) {
      return this.classOptions;
    }

    return this.classOptions.filter(
      (item) => `${item.gradeLevelId ?? ''}` === `${this.selectedStaffGradeId}`
    );
  }

  getSelectedStaffLabel(): string {
    if (!this.selectedStaff) return '';
    return `${this.selectedStaff.fullName ?? ''}`.trim();
  }

  private loadOptions(): void {
    this.loadStaffGroups();

    this.namHocService.getOptions().subscribe((res) => {
      this.findFormControl(this.$formItem, this.key.SCHOOL_YEAR_ID).options = (
        res.data ?? []
      ).map((item: NamHocOptionResponse) => ({
        label: item.name,
        value: item.id,
      })) as IOptions[];

      if (this.data.type === this.TYPE_FORM.CREATE) {
        this.namHocService.getCurrent().subscribe({
          next: ({ data }) => {
            const schoolYearControl = this.form.get(this.key.SCHOOL_YEAR_ID);
            const schoolYearValue = schoolYearControl?.value;
            if (
              data?.id != null &&
              (schoolYearValue == null || schoolYearValue === '')
            ) {
              schoolYearControl?.setValue(data.id, { emitEvent: true });
            }
          },
        });
      }
    });

    this.lopService.getOptions().subscribe((res) => {
      this.allClassOptions = (res.data ?? []).map((item: LopResponse) => ({
        label: item.name,
        value: item.id,
        gradeLevelId: item.gradeLevelId,
      }));
      this.reloadClassOptionsBySubject();
    });

    this.monHocService
      .filter({ pageNow: 1, pageSize: 1000 })
      .subscribe((res) => {
        const items = res.data?.items ?? res.data?.data ?? [];
        this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options =
          items.map((item: MonHocResponse) => ({
            label: item.name,
            value: item.id,
          })) as IOptions[];
        this.updateAssignmentSummary();
      });
  }

  private loadStaffGroups(): void {
    if (this.data.staffId) {
      this.canBoService.getById(this.data.staffId).subscribe({
        next: ({ data }) => {
          this.selectedStaff = data;
          this.selectedStaffGradeId = data.gradeId ?? undefined;
          this.staffGroups = [
            {
              gradeId: data.gradeId ?? 'grade',
              gradeName: data.gradeName ?? 'Cán bộ',
              staffs: [data],
              expanded: true,
            },
          ];
          this.reloadClassOptionsBySubject();
        },
      });
      return;
    }

    this.khoiService.getOptions().subscribe({
      next: ({ data }) => {
        const grades = data ?? [];
        if (!grades.length) {
          this.staffGroups = [];
          return;
        }

        forkJoin(
          grades.map((grade) => this.canBoService.getByGrade(grade.id))
        ).subscribe({
          next: (responses) => {
            this.staffGroups = grades.map((grade, index) => ({
              gradeId: grade.id,
              gradeName: grade.name,
              staffs: responses[index]?.data ?? [],
              expanded: index === 0,
            }));

            const firstStaff = this.staffGroups.find(
              (item) => item.staffs.length
            )?.staffs[0];
            if (firstStaff) {
              const group = this.staffGroups.find((item) =>
                item.staffs.some(
                  (staff) => `${staff.id}` === `${firstStaff.id}`
                )
              );
              this.selectStaff(firstStaff, group?.gradeId);
            }
          },
          error: () => {
            this.staffGroups = [];
          },
        });
      },
      error: () => {
        this.staffGroups = [];
      },
    });
  }

  private bindFormChanges(): void {
    this.form
      .get(this.key.SCHOOL_YEAR_ID)
      ?.valueChanges.pipe(distinctUntilChanged(), takeUntil(this.ngUnsubscribe))
      .subscribe((schoolYearId) => {
        this.form.patchValue(
          {
            [this.key.SEMESTER_ID]: null,
          },
          { emitEvent: false }
        );

        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];

        if (schoolYearId == null || schoolYearId === '') {
          return;
        }

        this.loadSemesterOptions(schoolYearId as ID_TYPE);
      });

    this.form.get(this.key.SUBJECT_ID)?.valueChanges.subscribe((subjectId) => {
      this.persistSelectionBySubject(this.previousSubjectId);
      this.previousSubjectId = subjectId;
      this.reloadClassOptionsBySubject();
    });
  }

  private loadSemesterOptions(schoolYearId: ID_TYPE): void {
    this.hocKyService.getOptions(schoolYearId).subscribe({
      next: ({ data }) => {
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = (
          data ?? []
        ).map((item: HocKyResponse | { id: number; name: string }) => ({
          label: item.name ?? '',
          value: item.id,
        })) as IOptions[];
      },
      error: () => {
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];
      },
    });
  }

  private patchData(
    data: Partial<PhanCongGiangDayResponse> & PhanCongGiangDayDetailResponse
  ): void {
    this.hydrateAssignmentSelections(data);
    const firstAssignment = data.assignments?.find((item) => item.subjectId);
    const activeSubjectId = data.subjectId ?? firstAssignment?.subjectId;
    const selectedClassIds = Array.isArray(data.classIds)
      ? data.classIds.filter(
          (item) => item !== null && item !== undefined && item !== ''
        )
      : data.classId != null && data.classId !== ''
        ? [data.classId]
        : [];
    const activeClassIds =
      selectedClassIds.length || !firstAssignment?.classIds?.length
        ? selectedClassIds
        : firstAssignment.classIds.filter(
            (item) => item !== null && item !== undefined && item !== ''
          );

    this.form.patchValue(
      {
        schoolYearId: data.schoolYearId ?? '',
        semesterId: data.semesterId ?? '',
        classId: activeClassIds,
        subjectId: activeSubjectId ?? '',
        departmentId: data.departmentId ?? '',
        assignmentSummary:
          this.buildAssignmentSummaryFromSelections() || data.className || '',
      },
      { emitEvent: false }
    );

    const patchedSchoolYearId =
      data.schoolYearId ?? this.form.get(this.key.SCHOOL_YEAR_ID)?.value;
    if (patchedSchoolYearId != null && patchedSchoolYearId !== '') {
      this.loadSemesterOptions(patchedSchoolYearId as ID_TYPE);
    }

    if (activeSubjectId != null && activeSubjectId !== '') {
      const subjectKey = this.toKey(activeSubjectId);
      const classIds = activeClassIds;

      if (classIds.length) {
        this.subjectClassSelections.set(subjectKey, classIds);
        if (!this.subjectOrder.includes(subjectKey)) {
          this.subjectOrder.push(subjectKey);
        }

        const labels = new Map<string, string>();
        if (data.className && classIds.length === 1) {
          labels.set(`${classIds[0]}`, data.className);
        }
        this.subjectClassLabelMap.set(subjectKey, labels);
      }
    }

    this.previousSubjectId = activeSubjectId;

    if (data.staffId != null && data.staffId !== '') {
      this.resolveSelectedStaff(data.staffId);
      return;
    }

    this.reloadClassOptionsBySubject();
  }

  private resolveSelectedStaff(staffId: ID_TYPE): void {
    const matchedGroup = this.staffGroups.find((group) =>
      group.staffs.some((staff) => `${staff.id}` === `${staffId}`)
    );
    const matchedStaff = matchedGroup?.staffs.find(
      (staff) => `${staff.id}` === `${staffId}`
    );
    if (matchedStaff) {
      matchedGroup!.expanded = true;
      this.selectStaff(matchedStaff, matchedGroup?.gradeId);
      return;
    }

    this.canBoService.getById(staffId).subscribe({
      next: ({ data }) => {
        this.selectedStaff = data;
        this.selectedStaffGradeId = data.gradeId ?? undefined;
        this.reloadClassOptionsBySubject();
      },
    });
  }

  private reloadClassOptionsBySubject(): void {
    const subjectId = this.form.get(this.key.SUBJECT_ID)?.value;

    if (subjectId == null || subjectId === '') {
      this.classOptions = [...this.allClassOptions];
      this.syncMissingSelectedClasses();
      this.filterClassOptionsByGrade();
      return;
    }

    this.phanCongGiaoVienService
      .getClassroomsBySubject(subjectId, {
        unitId: this.getCurrentUnitId(),
      })
      .subscribe({
        next: ({ data }) => {
          this.classOptions = this.mapClassroomOptions(data ?? []);
          this.syncMissingSelectedClasses();
          this.filterClassOptionsByGrade();
        },
        error: () => {
          this.classOptions = [...this.allClassOptions];
          this.syncMissingSelectedClasses();
          this.filterClassOptionsByGrade();
        },
      });
  }

  private mapClassroomOptions(
    items: PhanCongGiaoVienClassroomResponse[]
  ): ClassOption[] {
    const allClassMap = new Map(
      this.allClassOptions.map((item) => [`${item.value}`, item])
    );

    return items.map((item) => ({
      label: item.name,
      value: item.id,
      gradeLevelId: allClassMap.get(`${item.id}`)?.gradeLevelId,
    }));
  }

  private getCurrentUnitId(): ID_TYPE | undefined {
    return this.selectedStaff?.unitId ?? this.authService.currentUser?.unit?.id;
  }

  private filterClassOptionsByGrade(): void {
    const subjectKey = this.getCurrentSubjectKey();
    const selectedIds =
      this.isCreateMode() && subjectKey
        ? (this.subjectClassSelections.get(subjectKey) ?? [])
        : this.getSelectedClassIds();
    const displayedIds = this.getDisplayedClassOptions().map(
      (item) => `${item.value}`
    );
    const nextSelectedIds = selectedIds.filter((item) =>
      displayedIds.includes(`${item}`)
    );

    if (this.isCreateMode()) {
      this.form
        .get(this.key.CLASS_ID)
        ?.setValue(nextSelectedIds, { emitEvent: false });
      this.syncCurrentSubjectSelection(nextSelectedIds);
    } else if (nextSelectedIds.length) {
      this.form
        .get(this.key.CLASS_ID)
        ?.setValue(nextSelectedIds[0], { emitEvent: false });
    } else if (this.data.type !== this.TYPE_FORM.DETAIL) {
      this.form
        .get(this.key.CLASS_ID)
        ?.setValue(this.isCreateMode() ? [] : null, {
          emitEvent: false,
        });
    }

    this.updateAssignmentSummary();
  }

  private getSelectedClassIds(): ID_TYPE[] {
    const value = this.form.get(this.key.CLASS_ID)?.value;
    if (Array.isArray(value)) {
      return value.filter(
        (item) => item !== null && item !== undefined && item !== ''
      );
    }
    return value !== null && value !== undefined && value !== '' ? [value] : [];
  }

  private syncMissingSelectedClasses(): void {
    const subjectKey = this.getCurrentSubjectKey();
    const selectedIds =
      this.isCreateMode() && subjectKey
        ? (this.subjectClassSelections.get(subjectKey) ?? [])
        : this.getSelectedClassIds();
    const missingIds = selectedIds.filter(
      (item) =>
        !this.classOptions.some(
          (classItem) => `${classItem.value}` === `${item}`
        )
    );

    if (!missingIds.length) return;

    const fallbackLabel = `${this.data.data?.className ?? ''}`.trim();
    this.classOptions = [
      ...missingIds.map((item) => ({
        value: item,
        label: fallbackLabel || `${item}`,
      })),
      ...this.classOptions,
    ];
  }

  private hydrateAssignmentSelections(
    data: Partial<PhanCongGiangDayResponse> & PhanCongGiangDayDetailResponse
  ): void {
    const assignments = data.assignments ?? [];
    if (!assignments.length) return;

    this.subjectClassSelections.clear();
    this.subjectClassLabelMap.clear();
    this.subjectLabelMap.clear();
    this.subjectOrder = [];

    assignments.forEach((assignment) => {
      const subjectKey = this.toKey(assignment.subjectId);
      if (!subjectKey) return;

      const classIds = (assignment.classIds ?? []).filter(
        (item) => item !== null && item !== undefined && item !== ''
      );
      if (!classIds.length) return;

      this.subjectClassSelections.set(subjectKey, classIds);
      this.subjectOrder.push(subjectKey);
      if (assignment.subjectName) {
        this.subjectLabelMap.set(subjectKey, assignment.subjectName);
      }

      const labels = new Map<string, string>();
      (assignment.classNames ?? []).forEach((className, index) => {
        const classId = classIds[index];
        if (classId !== null && classId !== undefined && className) {
          labels.set(`${classId}`, className);
        }
      });
      this.subjectClassLabelMap.set(subjectKey, labels);
    });
  }

  private buildAssignmentSummaryFromSelections(): string {
    return this.subjectOrder
      .map((subjectKey) => {
        const classIds = this.subjectClassSelections.get(subjectKey) ?? [];
        if (!classIds.length) return '';

        const classLabels = classIds
          .map((classId) => this.getClassLabel(subjectKey, classId))
          .filter((item): item is string => !!item);
        if (!classLabels.length) return '';

        const subjectLabel = this.getSubjectLabel(subjectKey);
        return subjectLabel
          ? `${subjectLabel}(${classLabels.join(', ')})`
          : classLabels.join(', ');
      })
      .filter((item) => !!item)
      .join(' - ');
  }

  private updateAssignmentSummary(): void {
    if (this.data.type === this.TYPE_FORM.DETAIL && this.subjectOrder.length) {
      this.form
        .get(this.key.ASSIGNMENT_SUMMARY)
        ?.setValue(this.buildAssignmentSummaryFromSelections(), {
          emitEvent: false,
        });
      return;
    }

    if (this.isCreateMode()) {
      this.syncCurrentSubjectSelection();

      this.form
        .get(this.key.ASSIGNMENT_SUMMARY)
        ?.setValue(this.buildAssignmentSummaryFromSelections(), {
          emitEvent: false,
        });
      return;
    }

    const subjectId = this.form.get(this.key.SUBJECT_ID)?.value;
    const subjectLabel =
      this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options.find(
        (item) => `${item.value}` === `${subjectId}`
      )?.label ?? '';

    const classLabels = this.getSelectedClassIds()
      .map(
        (classId) =>
          this.classOptions.find((item) => `${item.value}` === `${classId}`)
            ?.label
      )
      .filter((item): item is string => !!item);

    const summary =
      subjectLabel && classLabels.length
        ? `${subjectLabel}(${classLabels.join(', ')})`
        : classLabels.join(', ');

    this.form
      .get(this.key.ASSIGNMENT_SUMMARY)
      ?.setValue(summary, { emitEvent: false });
  }

  private buildUpsertPayload(): PhanCongGiangDayUpsertRequest {
    const value = this.form.getRawValue();
    this.syncCurrentSubjectSelection();
    const basePayload = {
      unitId: this.getCurrentUnitId(),
      staffId: this.selectedStaff?.id ?? this.data.staffId,
      schoolYearId: value.schoolYearId ?? '',
      semesterId: value.semesterId ?? '',
    };

    if (this.isCreateMode()) {
      const assignments = this.subjectOrder
        .map((subjectKey) => {
          const classIdsBySubject =
            this.subjectClassSelections.get(subjectKey) ?? [];
          const classIds = classIdsBySubject.filter(
            (classId, index, list) =>
              list.findIndex((item) => `${item}` === `${classId}`) === index
          );

          return {
            subjectId: this.parseIdType(subjectKey),
            classIds,
          };
        })
        .filter((item) => item.classIds.length);

      return {
        ...basePayload,
        assignments,
      };
    }

    const subjectId = value.subjectId;
    const selectedClassIds = this.getSelectedClassIds();

    if (subjectId == null || subjectId === '') {
      return {
        ...basePayload,
        assignments: [],
      };
    }

    return {
      ...basePayload,
      assignments: [
        {
          subjectId,
          classIds: selectedClassIds,
        },
      ],
    };
  }

  private isCreateMode(): boolean {
    return this.data.type !== this.TYPE_FORM.DETAIL;
  }

  private getCurrentSubjectKey(): string {
    return this.toKey(this.form.get(this.key.SUBJECT_ID)?.value);
  }

  private syncCurrentSubjectSelection(selectedIds?: ID_TYPE[]): void {
    if (!this.isCreateMode()) return;
    this.persistSelectionBySubject(
      this.form.get(this.key.SUBJECT_ID)?.value,
      selectedIds
    );
  }

  private persistSelectionBySubject(
    subjectId?: ID_TYPE,
    selectedIds?: ID_TYPE[]
  ): void {
    const subjectKey = this.toKey(subjectId);
    if (!subjectKey) return;

    const classIds = (selectedIds ?? this.getSelectedClassIds()).filter(
      (item) => item !== null && item !== undefined && item !== ''
    );

    if (!classIds.length) {
      this.subjectClassSelections.delete(subjectKey);
      this.subjectClassLabelMap.delete(subjectKey);
      this.subjectOrder = this.subjectOrder.filter(
        (item) => item !== subjectKey
      );
      return;
    }

    this.subjectClassSelections.set(subjectKey, classIds);
    if (!this.subjectOrder.includes(subjectKey)) {
      this.subjectOrder.push(subjectKey);
    }

    const labelMap =
      this.subjectClassLabelMap.get(subjectKey) ?? new Map<string, string>();
    classIds.forEach((classId) => {
      const label = this.classOptions.find(
        (item) => `${item.value}` === `${classId}`
      )?.label;
      if (label) {
        labelMap.set(`${classId}`, label);
      }
    });
    this.subjectClassLabelMap.set(subjectKey, labelMap);
  }

  private getClassLabel(subjectKey: string, classId: ID_TYPE): string {
    const storedLabel = this.subjectClassLabelMap
      .get(subjectKey)
      ?.get(`${classId}`);
    if (storedLabel) return storedLabel;

    return (
      this.allClassOptions.find((item) => `${item.value}` === `${classId}`)
        ?.label ??
      this.classOptions.find((item) => `${item.value}` === `${classId}`)
        ?.label ??
      `${classId}`
    );
  }

  private getSubjectLabel(subjectKey: string): string {
    return (
      this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options.find(
        (item) => `${item.value}` === subjectKey
      )?.label ??
      this.subjectLabelMap.get(subjectKey) ??
      ''
    );
  }

  private toKey(value?: ID_TYPE): string {
    return value === null || value === undefined || value === ''
      ? ''
      : `${value}`;
  }

  private parseIdType(value: string): ID_TYPE {
    return /^\d+$/.test(value) ? Number(value) : value;
  }
}
