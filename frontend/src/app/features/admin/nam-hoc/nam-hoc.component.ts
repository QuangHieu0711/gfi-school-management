import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import {
  FormType,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';

import {
  BOOLEAN_OPTIONS,
  NAM_HOC_KEY,
  NamHocResponse,
  SCHOOL_YEAR_STATUS_OPTIONS,
} from '@app/model/admin/nam-hoc.model';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { DialogNamHocComponent } from './dialog-nam-hoc/dialog-nam-hoc.component';
import { DialogCauHinhHocKyComponent } from './dialog-cau-hinh-hoc-ky/dialog-cau-hinh-hoc-ky.component';

@Component({
  selector: 'nam-hoc',
  templateUrl: './nam-hoc.component.html',
  styleUrls: ['./nam-hoc.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class NamHocComponent extends ComponentBaseAbstract {
  @ViewChild('nameTpl', { static: true })
  nameTpl!: TemplateRef<unknown>;
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;
  @ViewChild('currentTpl', { static: true })
  currentTpl!: TemplateRef<unknown>;
  @ViewChild('periodTpl', { static: true })
  periodTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem: FormType[] = [
    TEXT_CONTROL({
      controlName: NAM_HOC_KEY.NAME,
      placeholder: 'Tìm kiếm theo mã hoặc tên năm học',
      required: false,
      maxLength: 255,
    }),
    SELECT_CONTROL({
      controlName: NAM_HOC_KEY.STATUS,
      placeholder: 'Trạng thái',
      required: false,
      clearable: true,
      listOption: SCHOOL_YEAR_STATUS_OPTIONS,
    }),
    SELECT_CONTROL({
      controlName: NAM_HOC_KEY.IS_CURRENT,
      placeholder: 'Hiện hành',
      required: false,
      clearable: true,
      listOption: BOOLEAN_OPTIONS,
    }),
  ];
  key = NAM_HOC_KEY;
  dataSource: NamHocResponse[] = [];

  constructor(
    protected override injector: Injector,
    private readonly namHocService: NamHocService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.columns = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Mã năm học',
        field: NAM_HOC_KEY.CODE,
      },
      {
        header: 'Tên năm học',
        field: NAM_HOC_KEY.NAME,
        cellTemplate: this.nameTpl,
      },
      {
        header: 'Thời gian',
        field: NAM_HOC_KEY.START_DATE,
        cellTemplate: this.periodTpl,
      },
      {
        header: 'Trạng thái',
        field: NAM_HOC_KEY.STATUS,
        class: 'text-center',
        cellTemplate: this.statusTpl,
      },
      {
        header: 'Hiện hành',
        field: NAM_HOC_KEY.IS_CURRENT,
        class: 'text-center',
        cellTemplate: this.currentTpl,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'settings',
            class: 'action-view',
            tooltip: 'Cấu hình học kỳ',
            click: (rowData: NamHocResponse) =>
              this.openSemesterConfig(rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (rowData: NamHocResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (rowData: NamHocResponse) => this.deleteSchoolYear(rowData),
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
    const keyword = formValues[NAM_HOC_KEY.NAME] ?? undefined;

    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        code: keyword,
        name: keyword,
        status: formValues[NAM_HOC_KEY.STATUS] ?? undefined,
        isCurrent:
          formValues[NAM_HOC_KEY.IS_CURRENT] === '' ||
          formValues[NAM_HOC_KEY.IS_CURRENT] === null ||
          formValues[NAM_HOC_KEY.IS_CURRENT] === undefined
            ? undefined
            : formValues[NAM_HOC_KEY.IS_CURRENT],
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.namHocService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách năm học',
          'Thất bại'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: NamHocResponse) {
    this.dialog.componentDialog(
      DialogNamHocComponent,
      {
        width: '720px',
        data: {
          type,
          id: rowData?.[NAM_HOC_KEY.ID],
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

  openSemesterConfig(rowData: NamHocResponse) {
    this.dialog.componentDialog(DialogCauHinhHocKyComponent, {
      width: '1100px',
      height: '680px',
      maxHeight: '85vh',
      panelClass: 'semester-config-dialog',
      data: {
        schoolYearId: rowData[NAM_HOC_KEY.ID],
        schoolYear: rowData,
      },
    });
  }

  deleteSchoolYear(rowData: NamHocResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa năm học ${rowData[NAM_HOC_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.namHocService.delete(rowData[NAM_HOC_KEY.ID]).subscribe({
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

  getStatusLabel(status?: number): string {
    return (
      SCHOOL_YEAR_STATUS_OPTIONS.find((item) => item.value === status)?.label ??
      '--'
    );
  }

  getStatusClass(status?: number): string {
    switch (status) {
      case 0:
        return 'planning';
      case 1:
        return 'active';
      case 2:
        return 'completed';
      default:
        return '';
    }
  }

  formatDateRange(start?: string, end?: string): string {
    return `${this.formatDate(start)} - ${this.formatDate(end)}`;
  }

  private formatDate(value?: string): string {
    if (!value) return '--';
    const raw = value.slice(0, 10);
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }
}
