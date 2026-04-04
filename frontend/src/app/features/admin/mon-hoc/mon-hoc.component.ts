import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  MON_HOC_FILTER_FORM,
  MON_HOC_KEY,
  MON_HOC_TYPE_OPTIONS,
  MonHocResponse,
} from '@app/model/admin/mon-hoc.model';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { DialogMonHocComponent } from './dialog-mon-hoc/dialog-mon-hoc.component';

@Component({
  selector: 'mon-hoc',
  templateUrl: './mon-hoc.component.html',
  styleUrls: ['./mon-hoc.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class MonHocComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = MON_HOC_FILTER_FORM;
  key = MON_HOC_KEY;
  dataSource: MonHocResponse[] = [];
  showAdvancedFilters = false;

  constructor(
    protected override injector: Injector,
    private readonly monHocService: MonHocService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.columns = [
      { header: 'STT', class: 'text-center', field: COMMON_TABLE_KEY.STT },
      { header: 'Mã môn học', field: MON_HOC_KEY.CODE },
      { header: 'Tên môn học', field: MON_HOC_KEY.NAME },
      {
        header: 'Loại',
        field: MON_HOC_KEY.TYPE,
        formatter: (data: MonHocResponse) =>
          this.getTypeLabel(data[MON_HOC_KEY.TYPE] as number),
      },
      {
        header: 'Trạng thái',
        field: MON_HOC_KEY.STATUS,
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
            class: 'action-view',
            tooltip: 'Chi tiết',
            click: (rowData: MonHocResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (rowData: MonHocResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (rowData: MonHocResponse) => this.deleteMonHoc(rowData),
          },
        ],
      },
    ];

    this.filterData({ pageIndex: 0, pageSize: this.pageSize });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    const formValues = this.form.getRawValue();
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        subject: formValues[MON_HOC_KEY.NAME] ?? undefined,
        type: formValues[MON_HOC_KEY.TYPE] ?? undefined,
        status: formValues[MON_HOC_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.monHocService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách môn học',
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

  openDialog(type: TYPE_FORM_KEY, rowData?: MonHocResponse) {
    this.dialog.componentDialog(
      DialogMonHocComponent,
      {
        width: '720px',
        data: {
          type,
          id: rowData?.[MON_HOC_KEY.ID],
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

  deleteMonHoc(rowData: MonHocResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa môn học ${rowData[MON_HOC_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.monHocService.delete(rowData[MON_HOC_KEY.ID]).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
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

  getTypeLabel(type?: number) {
    return (
      MON_HOC_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? '--'
    );
  }
}
