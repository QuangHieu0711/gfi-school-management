import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  KHOI_FILTER_FORM,
  KHOI_KEY,
  KhoiResponse,
} from '@app/model/admin/khoi.model';
import { KhoiService } from '@app/service/admin/khoi.service';
import { DialogCauHinhMonHocComponent } from './dialog-cau-hinh-mon-hoc/dialog-cau-hinh-mon-hoc.component';
import { DialogKhoiComponent } from './dialog-khoi/dialog-khoi.component';

@Component({
  selector: 'khoi',
  templateUrl: './khoi.component.html',
  styleUrls: ['./khoi.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class KhoiComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = KHOI_FILTER_FORM;
  key = KHOI_KEY;
  dataSource: KhoiResponse[] = [];

  constructor(
    protected override injector: Injector,
    private readonly khoiService: KhoiService
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
        header: 'Mã khối',
        field: KHOI_KEY.CODE,
      },
      {
        header: 'Tên khối',
        field: KHOI_KEY.NAME,
      },
      {
        header: 'Số khối',
        field: KHOI_KEY.GRADE_NUMBER,
        class: 'text-center',
      },
      {
        header: 'Mô tả',
        field: KHOI_KEY.DESCRIPTION,
      },
      {
        header: 'Trạng thái',
        field: KHOI_KEY.STATUS,
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
            tooltip: 'Chi tiết',
            click: (rowData: KhoiResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            tooltip: 'Chỉnh sửa',
            click: (rowData: KhoiResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'assignment',
            tooltip: 'Cấu hình môn học',
            click: (rowData: KhoiResponse) => this.openSubjectConfig(rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            tooltip: 'Xóa',
            click: (rowData: KhoiResponse) => this.deleteKhoi(rowData),
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
    const keyword = formValues[KHOI_KEY.NAME] ?? undefined;
    const payload = {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? 0) + 1,
      filter: {
        gradeLevel: keyword,
        status: formValues[KHOI_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.khoiService.filter(payload).subscribe({
      next: ({ data }) => {
        const items = Array.isArray(data) ? data : data?.items || [];
        this.dataSource = items;
        this.dataSourceTotal = Array.isArray(data)
          ? data.length
          : data?.recordTotal || items.length;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được danh sách khối',
          'Thất bại'
        );
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: KhoiResponse) {
    this.dialog.componentDialog(
      DialogKhoiComponent,
      {
        width: '720px',
        data: {
          type,
          id: rowData?.[KHOI_KEY.ID],
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

  openSubjectConfig(rowData: KhoiResponse) {
    this.dialog.componentDialog(DialogCauHinhMonHocComponent, {
      width: '980px',
      data: {
        gradeLevelId: rowData[KHOI_KEY.ID],
        gradeLevel: rowData,
      },
    });
  }

  deleteKhoi(rowData: KhoiResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa khối ${rowData[KHOI_KEY.NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.khoiService.delete(rowData[KHOI_KEY.ID]).subscribe({
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
                'Xóa không thành công',
              'Thất bại'
            );
          },
        });
      }
    );
  }
}
