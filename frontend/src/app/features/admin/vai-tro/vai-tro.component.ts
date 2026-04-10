import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { MtxGridColumn } from '@ng-matero/extensions/grid';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import {
  VAI_TRO_FILTER_FORM,
  VAI_TRO_KEY,
  VaiTroResponse,
} from '@app/model/admin/vai-tro.model';
import { VaiTroService } from '@app/service/admin/vai-tro.service';
import { DialogVaiTroComponent } from './dialog-vai-tro/dialog-vai-tro.component';
import { PermissionCheckService } from '@service';

@Component({
  selector: 'vai-tro',
  templateUrl: './vai-tro.component.html',
  styleUrls: ['./vai-tro.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class VaiTroComponent extends ComponentBaseAbstract {
  @ViewChild('statusTpl', { static: true })
  statusTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'ROLE_MANAGEMENT';
  tableConfig = {
    hasFilterPanel: true,
  };
  columns: MtxGridColumn[] = [];
  $formItem = VAI_TRO_FILTER_FORM;
  key = VAI_TRO_KEY;
  dataSource: VaiTroResponse[] = [];

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly vaiTroService: VaiTroService,
    private readonly permissionCheckService: PermissionCheckService
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
        header: 'Mã vai trò',
        field: VAI_TRO_KEY.CODE,
      },
      {
        header: 'Tên vai trò',
        field: VAI_TRO_KEY.ROLE_NAME,
      },
      {
        header: 'Trạng thái',
        field: VAI_TRO_KEY.STATUS,
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
            icon: 'settings',
            class: 'action-config',
            iif: () => this.permissionCheckService.canConfig(this.menuCode),
            tooltip: 'Cấu hình vai trò',
            click: (rowData: VaiTroResponse) =>
              this.openPermissionConfig(rowData),
          },
          {
            type: 'icon',
            icon: 'visibility',
            class: 'action-view',
            tooltip: 'Chi tiết',
            click: (rowData: VaiTroResponse) =>
              this.openDialog(this.TYPE_FORM.DETAIL, rowData),
          },
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            iif: () => this.permissionCheckService.canEdit(this.menuCode),
            tooltip: 'Chỉnh sửa',
            click: (rowData: VaiTroResponse) =>
              this.openDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            iif: () => this.permissionCheckService.canDelete(this.menuCode),
            tooltip: 'Xóa',
            click: (rowData: VaiTroResponse) => this.deleteRole(rowData),
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
        roleName: formValues[VAI_TRO_KEY.ROLE_NAME] ?? undefined,
        status: formValues[VAI_TRO_KEY.STATUS] ?? undefined,
      },
    };

    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.vaiTroService.filter(payload).subscribe({
      next: ({ data }) => {
        this.dataSource = data.items || [];
        this.dataSourceTotal = data.recordTotal || 0;
      },
      error: (error) => {
        const message =
          error?.error?.userMessage ??
          error?.error?.message ??
          'Không tải được danh sách vai trò';
        this.toastr.error(message, 'Thất bại');
      },
    });
  }

  resetFilter() {
    this.form.reset();
    this.appTableComponent.resetQuery();
  }

  openDialog(type: TYPE_FORM_KEY, rowData?: VaiTroResponse) {
    this.dialog.componentDialog(
      DialogVaiTroComponent,
      {
        width: '560px',
        data: {
          type,
          id: rowData?.[VAI_TRO_KEY.ID],
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

  openPermissionConfig(rowData: VaiTroResponse) {
    const roleId = rowData[VAI_TRO_KEY.ID];
    if (roleId == null) return;

    this.router.navigate([
      '/',
      this.navigatorEndpoint.ADMIN.BASE_PATH,
      this.navigatorEndpoint.ADMIN.VAI_TRO.BASE_PATH,
      'cau-hinh',
      roleId,
    ]);
  }

  deleteRole(rowData: VaiTroResponse) {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa vai trò ${rowData[VAI_TRO_KEY.ROLE_NAME]} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.vaiTroService.delete(rowData[VAI_TRO_KEY.ID]).subscribe({
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
}
