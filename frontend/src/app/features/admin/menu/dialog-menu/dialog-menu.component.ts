/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { SVG_ICONS } from '@constant/icons';
import { AppDialogComponent } from '@components/app-dialog/app-dialog.component';
import { FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { ICON_CONSTANTS } from '@utils/icon.constant';

import {
  MENU_FORM,
  MENU_KEY,
  MenuFormRequest,
  MenuOptionResponse,
  MenuResponse,
} from '@app/model/admin/menu.model';
import { MenuService } from '@app/service/admin/menu.service';

@Component({
  selector: 'dialog-menu',
  templateUrl: './dialog-menu.component.html',
  imports: [...MATERIAL_MODULE, ...FORM_CONTROL_MODULE, AppDialogComponent],
})
export class DialogMenuComponent extends ComponentBaseAbstract {
  $formItem: FormType[] = MENU_FORM;
  key = MENU_KEY;
  title = '';
  currentData: MenuResponse | null = null;
  private parentOptions: MenuOptionResponse[] = [];
  private readonly iconOptions = [
    ...Object.entries(ICON_CONSTANTS).map(([value, label]) => ({
      value,
      label: `${label} (${value})`,
    })),
    ...SVG_ICONS.map((name) => ({
      value: `svg:${name}`,
      label: `SVG: ${name}`,
    })),
  ];

  constructor(
    protected override injector: Injector,
    private readonly dialogRef: MatDialogRef<DialogMenuComponent>,
    private readonly menuService: MenuService,
    @Inject(MAT_DIALOG_DATA)
    public data: { type: TYPE_FORM_KEY; id?: ID_TYPE; data?: MenuResponse } = {
      type: TYPE_FORM.CREATE,
    }
  ) {
    super(injector);
    this.form = this.itemControl.toFormGroup(this.$formItem);
  }

  protected override componentInit(): void {
    this.findFormControl(this.$formItem, MENU_KEY.ICON).options =
      this.iconOptions;

    switch (this.data.type) {
      case this.TYPE_FORM.UPDATE:
        this.title = 'Chỉnh sửa chức năng';
        break;
      case this.TYPE_FORM.DETAIL:
        this.title = 'Chi tiết chức năng';
        this.form.disable();
        break;
      default:
        this.title = 'Thêm mới chức năng';
        break;
    }

    if (this.data.type !== this.TYPE_FORM.CREATE && this.data.id != null) {
      forkJoin({
        detail: this.menuService.getById(this.data.id),
        options: this.menuService.getOptionsFresh(),
      }).subscribe({
        next: ({ detail, options }) => {
          this.parentOptions = this.normalizeParentOptions(options.data ?? []);
          this.currentData = this.normalizeMenuResponse(detail.data);
          this.form.patchValue(this.normalizeFormData(this.currentData));
          this.applyParentOptions();
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
    } else {
      this.loadParentOptions();
    }
  }

  onSubmit() {
    const formValue = this.form.getRawValue();
    const parentId = formValue[MENU_KEY.PARENT_ID];
    const payload: MenuFormRequest = {
      [MENU_KEY.PARENT_ID]: parentId === '' ? null : (parentId ?? null),
      [MENU_KEY.CODE]: formValue[MENU_KEY.CODE],
      [MENU_KEY.NAME]: formValue[MENU_KEY.NAME],
      [MENU_KEY.ICON]: formValue[MENU_KEY.ICON] || null,
      [MENU_KEY.URL]: formValue[MENU_KEY.URL] || null,
      [MENU_KEY.ORDINAL]: Number(formValue[MENU_KEY.ORDINAL]),
    };

    if (this.data.type === TYPE_FORM.CREATE) {
      this.handleCreate(payload);
      return;
    }

    this.handleUpdate(payload);
  }

  switchUpdate() {
    this.form.enable();
    this.title = 'Chỉnh sửa chức năng';
    this.data.type = this.TYPE_FORM.UPDATE;
  }

  private loadParentOptions() {
    this.menuService.getOptionsFresh().subscribe({
      next: ({ data }) => {
        this.parentOptions = this.normalizeParentOptions(data ?? []);
        this.applyParentOptions();
      },
      error: () => {
        this.findFormControl(this.$formItem, MENU_KEY.PARENT_ID).options = [
          {
            value: '',
            label: 'Không có menu cha',
          },
        ];
      },
    });
  }

  private applyParentOptions() {
    const currentId = this.currentData?.[MENU_KEY.ID];
    const currentParentId =
      this.form.getRawValue()?.[MENU_KEY.PARENT_ID] ??
      this.currentData?.[MENU_KEY.PARENT_ID];

    this.findFormControl(this.$formItem, MENU_KEY.PARENT_ID).options = [
      {
        value: '',
        label: 'Không có menu cha',
      },
      ...this.parentOptions
        .filter((item) => item.id !== currentId)
        .map((item) => ({
          value: item.id,
          label: this.getParentOptionLabel(item),
        })),
    ];

    if (
      currentParentId != null &&
      !this.findFormControl(this.$formItem, MENU_KEY.PARENT_ID).options.some(
        (option) => option.value === currentParentId
      )
    ) {
      this.form.patchValue({
        [MENU_KEY.PARENT_ID]: '',
      });
    }
  }

  private normalizeFormData(data: MenuResponse) {
    const parentId =
      data[MENU_KEY.PARENT_ID] ??
      this.parentOptions.find(
        (item) => item.code === data[MENU_KEY.PARENT_CODE]
      )?.id ??
      null;

    return {
      ...data,
      [MENU_KEY.PARENT_ID]: parentId ?? '',
      [MENU_KEY.ICON]: data[MENU_KEY.ICON] ?? null,
      [MENU_KEY.ORDINAL]: data[MENU_KEY.ORDINAL] ?? 1,
      [MENU_KEY.URL]: data[MENU_KEY.URL] ?? '',
    };
  }

  private normalizeMenuResponse(raw: any): MenuResponse {
    return {
      ...raw,
      [MENU_KEY.ID]: raw?.id ?? raw?.menuId,
      [MENU_KEY.CODE]: raw?.code ?? raw?.menuCode,
      [MENU_KEY.NAME]: raw?.name ?? raw?.menuName,
      [MENU_KEY.ICON]: raw?.icon ?? raw?.menuIcon ?? null,
      [MENU_KEY.URL]: raw?.url ?? raw?.menuUrl ?? null,
      [MENU_KEY.ORDINAL]: raw?.ordinal ?? raw?.sortOrder ?? 1,
      [MENU_KEY.PARENT_ID]: raw?.parentId ?? raw?.parentMenuId ?? null,
      [MENU_KEY.PARENT_CODE]:
        raw?.parentCode ?? raw?.menuParentCode ?? raw?.parentMenuCode ?? null,
    };
  }

  private normalizeParentOptions(items: any[]): MenuOptionResponse[] {
    const normalized = (items ?? []).map((raw) => ({
      id: raw?.id ?? raw?.menuId,
      code: raw?.code ?? raw?.menuCode ?? null,
      name: raw?.name ?? raw?.menuName ?? '',
      parentId: raw?.parentId ?? raw?.parentMenuId ?? null,
      parentCode: raw?.parentCode ?? raw?.parentMenuCode ?? null,
    }));

    const codeToId = new Map(
      normalized
        .filter((item) => item.code != null && item.id != null)
        .map((item) => [item.code, item.id as ID_TYPE])
    );

    return normalized
      .filter((item) => item.id != null && item.name)
      .map((item) => ({
        id: item.id as ID_TYPE,
        code: item.code,
        name: item.name,
        parentId:
          item.parentId ??
          (item.parentCode ? (codeToId.get(item.parentCode) ?? null) : null),
      }));
  }

  private getParentOptionLabel(item: MenuOptionResponse): string {
    const codeLabel = item.code ? ` (${item.code})` : '';
    return item.parentId != null ? `${item.name}${codeLabel}` : item.name;
  }

  private handleCreate(payload: MenuFormRequest) {
    this.menuService.create(payload).subscribe({
      next: () => {
        this.toastr.success('Lưu thành công', 'Thành công');
        this.menuService.notifyMenuChanged();
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu thất bại',
          'Thất bại'
        );
      },
    });
  }

  private handleUpdate(payload: MenuFormRequest) {
    this.menuService.update(this.data.id!, payload).subscribe({
      next: () => {
        this.toastr.success('Cập nhật thành công', 'Thành công');
        this.menuService.notifyMenuChanged();
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Cập nhật thất bại',
          'Thất bại'
        );
      },
    });
  }
}
