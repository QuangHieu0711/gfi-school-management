import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  LOP_FILTER_FORM,
  LOP_KEY,
  LopResponse,
} from '@app/model/admin/lop.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { KhoiService } from '@app/service/admin/khoi.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { DialogCauHinhMonHocLopComponent } from './dialog-cau-hinh-mon-hoc/dialog-cau-hinh-mon-hoc.component';
import { DialogLopComponent } from './dialog-lop/dialog-lop.component';

@Component({
  selector: 'lop',
  templateUrl: './lop.component.html',
  styleUrls: ['./lop.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class LopComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = LOP_FILTER_FORM;
  key = LOP_KEY;
  dataSource: LopResponse[] = [];
  showAdvancedFilters = false;

  constructor(
    protected override injector: Injector,
    private readonly lopService: LopService,
    private readonly donViService: DonViService,
    private readonly khoiService: KhoiService,
    private readonly namHocService: NamHocService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.loadSelectOptions();

    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Mã lớp',
        field: LOP_KEY.CODE,
      },
      {
        header: 'Tên lớp',
        field: LOP_KEY.NAME,
      },
      {
        header: 'Tên đơn vị',
        field: LOP_KEY.UNIT_NAME,
      },
      {
        header: 'Khối',
        field: LOP_KEY.GRADE_LEVEL_NAME,
      },
      {
        header: 'Năm học',
        field: LOP_KEY.SCHOOL_YEAR_NAME,
      },
      {
        header: 'Trạng thái',
        field: LOP_KEY.STATUS,
        class: 'text-center',
        cellTemplate: this.statusTpl,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'visibility',
            class: 'action-config',
            tooltip: 'Chi tiết',
            click: (rowData: LopResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (rowData: LopResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'assignment',
            class: 'action-view',
            tooltip: 'Cấu hình môn học',
            click: (rowData: LopResponse) => this.openSubjectConfig(rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (rowData: LopResponse) => this.deleteLop(rowData),
          },
        ],
      },
    ];

    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    const formValues = this.form.getRawValue();
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        className: formValues[LOP_KEY.NAME] ?? undefined,
        unitId: formValues[LOP_KEY.UNIT_ID] ?? undefined,
        gradeLevelId: formValues[LOP_KEY.GRADE_LEVEL_ID] ?? undefined,
        schoolYearId: formValues[LOP_KEY.SCHOOL_YEAR_ID] ?? undefined,
        status: formValues[LOP_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.lopService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách lớp',
          'Thất bại'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  toggleAdvancedFilters() {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: LopResponse) {
    this.dialog.componentDialog(
      DialogLopComponent,
      {
        width: '720px',
        data: {
          type,
          id: rowData?.[LOP_KEY.ID],
          data: rowData,
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

  openSubjectConfig(rowData: LopResponse) {
    this.dialog.componentDialog(DialogCauHinhMonHocLopComponent, {
      width: '980px',
      data: {
        classroomId: rowData[LOP_KEY.ID],
        classroom: rowData,
      },
    });
  }

  deleteLop(rowData: LopResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa lớp ${rowData[LOP_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.lopService.delete(rowData[LOP_KEY.ID]).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');

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
                'Xóa thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  private loadSelectOptions() {
    this.donViService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.UNIT_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.khoiService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.GRADE_LEVEL_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.namHocService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, LOP_KEY.SCHOOL_YEAR_ID).options = (
        data ?? []
      ).map((item) => ({
        value: item.id,
        label: item.name,
      }));
    });
  }
}
