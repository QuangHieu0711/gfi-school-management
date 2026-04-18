import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { PermissionCheckService } from '@service';

import {
  PHAN_PHOI_CHUONG_TRINH_FILTER_FORM,
  PHAN_PHOI_CHUONG_TRINH_KEY,
  PhanPhoiChuongTrinhFilterRequest,
  PhanPhoiChuongTrinhResponse,
} from '@app/model/admin/phan-phoi-chuong-trinh.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { MonHocOptionResponse } from '@app/model/admin/mon-hoc.model';
import { DialogPhanPhoiChuongTrinhComponent } from './dialog-phan-phoi-chuong-trinh/dialog-phan-phoi-chuong-trinh.component';
import { LopService } from '@app/service/admin/lop.service';
import { MonHocService } from '@app/service/admin/mon-hoc.service';
import { PhanPhoiChuongTrinhService } from '@app/service/admin/phan-phoi-chuong-trinh.service';

@Component({
  selector: 'phan-phoi-chuong-trinh',
  standalone: true,
  templateUrl: './phan-phoi-chuong-trinh.component.html',
  styleUrls: ['./phan-phoi-chuong-trinh.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class PhanPhoiChuongTrinhComponent extends ComponentBaseAbstract {
  @ViewChild('lessonTpl', { static: true })
  lessonTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'CURRICULUM_DISTRIBUTION';
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = structuredClone(PHAN_PHOI_CHUONG_TRINH_FILTER_FORM);
  key = PHAN_PHOI_CHUONG_TRINH_KEY;
  dataSource: PhanPhoiChuongTrinhResponse[] = [];

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canConfigWeek(): boolean {
    return this.permissionCheckService.canConfig(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly phanPhoiChuongTrinhService: PhanPhoiChuongTrinhService,
    private readonly lopService: LopService,
    private readonly monHocService: MonHocService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.columns = [
      { header: 'STT', class: 'text-center', field: COMMON_TABLE_KEY.STT },
      { header: 'Tuần', field: this.key.WEEK, class: 'text-center' },
      { header: 'Tên lớp', field: this.key.CLASS_NAME },
      { header: 'Môn học', field: this.key.SUBJECT_NAME },
      { header: 'Phân môn', field: this.key.SUB_SUBJECT },
      { header: 'Tiết PPCT', field: this.key.PERIOD },
      {
        header: 'Tên bài học',
        field: this.key.LESSON_NAME,
        cellTemplate: this.lessonTpl,
      },
      { header: 'Ghi chú', field: this.key.NOTE },
      {
        header: 'Thao tác',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'visibility',
            class: 'action-view',
            tooltip: 'Chi tiết',
            iif: () => this.permissionCheckService.canView(this.menuCode),
            click: (rowData: PhanPhoiChuongTrinhResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            click: (rowData: PhanPhoiChuongTrinhResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            click: (rowData: PhanPhoiChuongTrinhResponse) =>
              this.deleteItem(rowData),
          },
        ],
      },
    ];

    this.loadOptions();
    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent): void {
    const payload = this.buildFilterPayload(pageChangeEvent);

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.phanPhoiChuongTrinhService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || data.data || [];
        this.dataSourceTotal = data.recordTotal || this.dataSource.length;
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được dữ liệu',
          'Thất bại'
        );
      },
    });
  }

  resetFilter(): void {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: PhanPhoiChuongTrinhResponse): void {
    this.dialog.componentDialog(
      DialogPhanPhoiChuongTrinhComponent,
      {
        width: '640px',
        data: {
          type,
          id: rowData?.id,
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

  openWeekConfig(): void {
    this.router.navigate([
      '/',
      this.navigatorEndpoint.ADMIN.BASE_PATH,
      this.navigatorEndpoint.ADMIN.CURRICULUM_DISTRIBUTION.BASE_PATH,
      'cau-hinh-tuan',
    ]);
  }

  deleteItem(rowData: PhanPhoiChuongTrinhResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa bài học ${rowData[this.key.LESSON_NAME] ?? ''} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.phanPhoiChuongTrinhService.delete(rowData[this.key.ID]).subscribe({
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

  private buildFilterPayload(
    pageChangeEvent?: TableQueryEvent
  ): PhanPhoiChuongTrinhFilterRequest {
    const formValues = this.form.getRawValue();

    return {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? this.pageIndex) + 1,
      filter: {
        week: formValues[this.key.WEEK] ?? undefined,
        classId: formValues[this.key.CLASS_ID] ?? undefined,
        subjectId: formValues[this.key.SUBJECT_ID] ?? undefined,
        lessonName: formValues[this.key.LESSON_NAME] ?? undefined,
      },
    };
  }

  private loadOptions(): void {
    this.lopService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options = (
        data ?? []
      ).map((item: LopResponse) => ({
        value: item.id,
        label: item.name,
      }));
    });

    this.monHocService.getOptions().subscribe(({ data }) => {
      this.findFormControl(this.$formItem, this.key.SUBJECT_ID).options = (
        data ?? []
      ).map((item: MonHocOptionResponse) => ({
        value: item.id,
        label: item.name,
      }));
    });
  }
}
