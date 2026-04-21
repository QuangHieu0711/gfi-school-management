import { Component, Injector } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, takeUntil } from 'rxjs';

import { AppPaginatorComponent } from '@components/app-paginator/app-paginator.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { FormType, SELECT_CONTROL } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { AuthService, PermissionCheckService } from '@service';

import {
  PHAN_CONG_GIANG_DAY_KEY,
  PhanCongGiangDayFilterRequest,
  PhanCongGiangDayResponse,
} from '@app/model/admin/phan-cong-giang-day.model';
import { HocKyResponse, HOC_KY_KEY } from '@app/model/admin/hoc-ky.model';
import { MonHocResponse } from '@app/model/admin/mon-hoc.model';
import { CanBoService } from '@app/service/admin/can-bo.service';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';
import { DialogPhanCongGiangDayComponent } from './dialog-phan-cong-giang-day/dialog-phan-cong-giang-day.component';

@Component({
  selector: 'phan-cong-giang-day-page',
  standalone: true,
  templateUrl: './phan-cong-giang-day.component.html',
  styleUrls: ['./phan-cong-giang-day.component.scss'],
  imports: [
    AppPaginatorComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class PhanCongGiangDayComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'ASSIGNMENT_LIST';
  readonly key = PHAN_CONG_GIANG_DAY_KEY;

  dataSource: PhanCongGiangDayResponse[] = [];
  selectedStaffId?: string;
  selectedStaffCode?: string;
  staffDisplayName = '';
  selectedUnitId?: string | number;

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.SCHOOL_YEAR_ID,
      placeholder: 'Năm học',
      required: false,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.SUBJECT_ID,
      placeholder: 'Môn học',
      required: false,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.SEMESTER_ID,
      placeholder: 'Học kỳ',
      required: false,
      clearable: true,
      listOption: [],
    }),
  ];

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canView(): boolean {
    return this.permissionCheckService.canView(this.menuCode);
  }

  get canEdit(): boolean {
    return this.permissionCheckService.canEdit(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly routeService: ActivatedRoute,
    private readonly routerService: Router,
    private readonly phanCongGiangDayService: PhanCongGiangDayService,
    private readonly authService: AuthService,
    private readonly namHocService: NamHocService,
    private readonly hocKyService: HocKyService,
    private readonly monHocService: MonHocService,
    private readonly canBoService: CanBoService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.selectedStaffId =
      this.routeService.snapshot.queryParamMap.get('staffId') ?? undefined;
    this.staffDisplayName =
      this.routeService.snapshot.queryParamMap.get('staffName') ?? '';
    this.selectedUnitId = this.authService.currentUser?.unit?.id;

    this.loadFilterOptions();
    this.watchSchoolYearChanges();
    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent): void {
    const payload = this.buildFilterPayload(pageChangeEvent);

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.phanCongGiangDayService.filter(payload).subscribe({
      next: ({ data }) => {
        const items = data.items || data.data || [];
        this.dataSource = this.buildTableRows(items);
        this.dataSourceTotal =
          data.totalItems || data.recordTotal || items.length;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách phân công giảng dạy',
          'Thất bại'
        );
      },
    });
  }

  resetFilter(): void {
    this.form.reset();
    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  onPageChange(event: PageEvent): void {
    this.filterData({
      pageIndex: event.pageIndex,
      pageSize: event.pageSize,
    });
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: PhanCongGiangDayResponse): void {
    this.dialog.componentDialog(
      DialogPhanCongGiangDayComponent,
      {
        width: '1080px',
        data: {
          type,
          id: rowData?.id,
          data: rowData,
          staffId: rowData?.staffId ?? this.selectedStaffId,
        },
      },
      (result?: boolean) => {
        if (result) {
          this.filterData({
            pageIndex: this.pageIndex,
            pageSize: this.pageSize,
          });
        }
      }
    );
  }

  goToStaffProfile(): void {
    if (!this.selectedStaffId) return;

    this.routerService.navigate(
      [
        '/',
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
        PATH.CHI_TIET,
        this.selectedStaffId,
      ],
      {
        queryParams: { tab: 'thong-tin-can-bo' },
      }
    );
  }

  trackByRow(_: number, row: PhanCongGiangDayResponse): string | number {
    return row.id;
  }

  private buildFilterPayload(
    pageChangeEvent?: TableQueryEvent
  ): PhanCongGiangDayFilterRequest {
    const formValues = this.form.getRawValue();

    return {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? this.pageIndex) + 1,
      filter: {
        unitId: this.selectedUnitId,
        schoolYearId: formValues[this.key.SCHOOL_YEAR_ID] ?? undefined,
        semesterId: formValues[this.key.SEMESTER_ID] ?? undefined,
        staffCode: this.selectedStaffCode ?? undefined,
        subjectId: formValues[this.key.SUBJECT_ID] ?? undefined,
      },
    };
  }

  private loadFilterOptions(): void {
    this.namHocService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.SCHOOL_YEAR_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));

      const schoolYearId =
        this.form.getRawValue()[this.key.SCHOOL_YEAR_ID] ?? undefined;
      if (schoolYearId != null && schoolYearId !== '') {
        this.loadSemesterOptions(schoolYearId as ID_TYPE);
      }
    });

    this.monHocService
      .filter({ pageNow: 1, pageSize: 1000 })
      .subscribe(({ data }) => {
        const items = data.items || data.data || [];
        this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options =
          items.map((item: MonHocResponse) => ({
            value: item.id,
            label: item.name,
          }));
      });

    if (this.selectedStaffId) {
      this.canBoService.getById(this.selectedStaffId).subscribe({
        next: ({ data }) => {
          this.selectedStaffCode = data.staffCode ?? undefined;
          this.selectedUnitId = data.unitId ?? this.selectedUnitId;
          this.staffDisplayName = data.fullName ?? data.staffCode ?? '';
        },
      });
    }
  }

  private loadSemesterOptions(schoolYearId: ID_TYPE): void {
    this.hocKyService.getOptions(schoolYearId).subscribe({
      next: ({ data }) => {
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = (
          data ?? []
        ).map((item: HocKyResponse | { id: number; name: string }) => ({
          value: item.id ?? item[HOC_KY_KEY.ID],
          label: item.name ?? item[HOC_KY_KEY.NAME] ?? '',
        }));
      },
      error: () => {
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];
      },
    });
  }

  private watchSchoolYearChanges(): void {
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

        if (schoolYearId == null || schoolYearId === '') {
          this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options =
            [];
          return;
        }

        this.loadSemesterOptions(schoolYearId as ID_TYPE);
      });
  }

  private buildTableRows(
    items: PhanCongGiangDayResponse[]
  ): PhanCongGiangDayResponse[] {
    const pageStart = this.pageIndex * this.pageSize;
    const groupedRows = new Map<string, PhanCongGiangDayResponse>();
    const staffOrder = new Map<string, number>();
    let staffCounter = 0;

    items.forEach((item) => {
      const normalizedAssignments = item.assignments?.length
        ? item.assignments.map((assignment) => ({
            ...item,
            subjectId: assignment.subjectId,
            subjectName: assignment.subjectName,
            classIds: assignment.classIds,
            classNames: assignment.classNames,
          }))
        : [item];

      normalizedAssignments.forEach((assignment) => {
        const staffKey = `${assignment.unitId ?? ''}-${assignment.staffId ?? ''}`;
        if (!staffOrder.has(staffKey)) {
          staffCounter += 1;
          staffOrder.set(staffKey, pageStart + staffCounter);
        }

        const subjectKey = `${staffKey}-${assignment.schoolYearId ?? ''}-${assignment.subjectId ?? ''}`;
        const existingRow = groupedRows.get(subjectKey);
        const nextClassNames = this.collectClassNames(assignment);
        const nextClassIds = this.collectClassIds(assignment);

        if (!existingRow) {
          groupedRows.set(subjectKey, {
            ...assignment,
            id: assignment.id ?? subjectKey,
            groupStt: staffOrder.get(staffKey),
            classNames: nextClassNames,
            classIds: nextClassIds,
          } as PhanCongGiangDayResponse);
          return;
        }

        existingRow.classNames = this.mergeUniqueValues(
          existingRow.classNames,
          nextClassNames
        );
        existingRow['classIds'] = this.mergeUniqueValues(
          existingRow['classIds'],
          nextClassIds
        );
      });
    });

    return this.markGroupHeads(Array.from(groupedRows.values()));
  }

  private collectClassNames(item: PhanCongGiangDayResponse): string[] {
    if (Array.isArray(item.classNames) && item.classNames.length) {
      return item.classNames.filter((value): value is string => !!value);
    }

    return item.className ? [item.className] : [];
  }

  private collectClassIds(item: PhanCongGiangDayResponse): (string | number)[] {
    if (Array.isArray(item['classIds']) && item['classIds'].length) {
      return item['classIds'].filter(
        (value): value is string | number =>
          value !== null && value !== undefined && value !== ''
      );
    }

    return item.classId !== null &&
      item.classId !== undefined &&
      item.classId !== ''
      ? [item.classId]
      : [];
  }

  private mergeUniqueValues<T>(current?: T[], incoming?: T[]): T[] {
    const result: T[] = [];

    [...(current ?? []), ...(incoming ?? [])].forEach((value) => {
      if (!result.some((item) => `${item}` === `${value}`)) {
        result.push(value);
      }
    });

    return result;
  }

  private markGroupHeads(
    rows: PhanCongGiangDayResponse[]
  ): PhanCongGiangDayResponse[] {
    const rowCountByStaff = new Map<string, number>();
    const groupStripeByStaff = new Map<string, boolean>();

    rows.forEach((row) => {
      const staffKey = `${row.unitId ?? ''}-${row.staffId ?? ''}`;
      rowCountByStaff.set(staffKey, (rowCountByStaff.get(staffKey) ?? 0) + 1);
    });

    let previousStaffKey = '';
    let groupIndex = -1;

    return rows.map((row) => {
      const currentStaffKey = `${row.unitId ?? ''}-${row.staffId ?? ''}`;
      const isGroupHead = currentStaffKey !== previousStaffKey;
      if (isGroupHead) {
        groupIndex += 1;
        groupStripeByStaff.set(currentStaffKey, groupIndex % 2 === 1);
      }
      previousStaffKey = currentStaffKey;

      return {
        ...row,
        isGroupHead,
        groupRowSpan: isGroupHead
          ? (rowCountByStaff.get(currentStaffKey) ?? 1)
          : 0,
        isStripedGroup: groupStripeByStaff.get(currentStaffKey) ?? false,
      };
    });
  }
}
