import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { FormType, SELECT_CONTROL } from '@model/form-control.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { PermissionCheckService } from '@service';

import {
  PHAN_CONG_GIANG_DAY_KEY,
  PhanCongGiangDayFilterRequest,
  PhanCongGiangDayResponse,
} from '@app/model/admin/phan-cong-giang-day.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { MonHocResponse } from '@app/model/admin/mon-hoc.model';
import { CanBoService } from '@app/service/admin/can-bo.service';
import { DonViService } from '@app/service/admin/don-vi.service';
import { LopService } from '@app/service/admin/lop.service';
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
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class PhanCongGiangDayComponent extends ComponentBaseAbstract {
  @ViewChild('homeroomTpl', { static: true })
  homeroomTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'ASSIGNMENT_LIST';
  readonly key = PHAN_CONG_GIANG_DAY_KEY;
  readonly tableConfig = {
    hasFilterPanel: true,
  };

  columns: MtxGridColumn[] = [];
  dataSource: PhanCongGiangDayResponse[] = [];
  selectedStaffId?: string;
  staffDisplayName = '';

  $formItem: FormType[] = [
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.SCHOOL_YEAR_ID,
      placeholder: 'Nam hoc',
      required: false,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.CLASS_ID,
      placeholder: 'Lop hoc',
      required: false,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.SUBJECT_ID,
      placeholder: 'Mon hoc',
      required: false,
      clearable: true,
      listOption: [],
    }),
    SELECT_CONTROL({
      controlName: PHAN_CONG_GIANG_DAY_KEY.DEPARTMENT_ID,
      placeholder: 'To/Phong ban',
      required: false,
      clearable: true,
      listOption: [],
    }),
  ];

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly routeService: ActivatedRoute,
    private readonly routerService: Router,
    private readonly phanCongGiangDayService: PhanCongGiangDayService,
    private readonly namHocService: NamHocService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    private readonly donViService: DonViService,
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

    this.columns = [
      { header: 'STT', class: 'text-center', field: COMMON_TABLE_KEY.STT },
      { header: 'Nam hoc', field: this.key.SCHOOL_YEAR_NAME },
      { header: 'Lop', field: this.key.CLASS_NAME },
      { header: 'Mon hoc', field: this.key.SUBJECT_NAME },
      { header: 'To/Phong ban', field: this.key.DEPARTMENT_NAME },
      { header: 'So tiet', field: this.key.TEACHING_LOAD, class: 'text-center' },
      {
        header: 'Chu nhiem',
        field: this.key.IS_HOMEROOM,
        class: 'text-center',
        cellTemplate: this.homeroomTpl,
      },
      { header: 'Ghi chu', field: this.key.NOTE },
      {
        header: 'Hanh dong',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'visibility',
            class: 'action-view',
            tooltip: 'Chi tiet',
            iif: () => this.permissionCheckService.canView(this.menuCode),
            click: (rowData: PhanCongGiangDayResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chinh sua',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            click: (rowData: PhanCongGiangDayResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xoa',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            click: (rowData: PhanCongGiangDayResponse) =>
              this.deleteTeachingAssignment(rowData),
          },
        ],
      },
    ];

    this.loadFilterOptions();
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
        this.dataSource = data.items || data.data || [];
        this.dataSourceTotal = data.recordTotal || this.dataSource.length;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Khong tai duoc danh sach phan cong giang day',
          'That bai'
        );
      },
    });
  }

  resetFilter(): void {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: PhanCongGiangDayResponse): void {
    this.dialog.componentDialog(
      DialogPhanCongGiangDayComponent,
      {
        width: '720px',
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

  deleteTeachingAssignment(rowData: PhanCongGiangDayResponse): void {
    this.dialog.confirm(
      {
        title: 'Xac nhan',
        message: 'Ban co chac chan muon xoa phan cong giang day nay khong?',
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.phanCongGiangDayService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xoa thanh cong', 'Thanh cong');
            if (this.dataSource.length === 1 && this.pageIndex > 0) {
              this.pageIndex = this.pageIndex - 1;
            }
            this.filterData({
              pageIndex: this.pageIndex,
              pageSize: this.pageSize,
            });
          },
          error: (error) => {
            this.toastr.error(
              error?.error?.userMessage ??
                error?.error?.message ??
                'Xoa that bai',
              'That bai'
            );
          },
        });
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

  private buildFilterPayload(
    pageChangeEvent?: TableQueryEvent
  ): PhanCongGiangDayFilterRequest {
    const formValues = this.form.getRawValue();

    return {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? this.pageIndex) + 1,
      filter: {
        staffId: this.selectedStaffId ?? undefined,
        schoolYearId: formValues[this.key.SCHOOL_YEAR_ID] ?? undefined,
        classId: formValues[this.key.CLASS_ID] ?? undefined,
        subjectId: formValues[this.key.SUBJECT_ID] ?? undefined,
        departmentId: formValues[this.key.DEPARTMENT_ID] ?? undefined,
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
    });

    this.lopService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options = (
        data ?? []
      ).map((item: LopResponse) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.monHocService.filter({ pageNow: 1, pageSize: 1000 }).subscribe(({ data }) => {
      const items = data.items || data.data || [];
      this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options =
        items.map((item: MonHocResponse) => ({
          value: item.id,
          label: item.name,
        }));
    });

    this.donViService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.DEPARTMENT_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    if (this.selectedStaffId && !this.staffDisplayName) {
      this.canBoService.getById(this.selectedStaffId).subscribe({
        next: ({ data }) => {
          this.staffDisplayName = data.fullName ?? data.staffCode ?? '';
        },
      });
    }
  }
}
