import { Component, Inject, Injector } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
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
    this.findFormControl(this.$formItem, MENU_KEY.ICON).options = this.iconOptions;
    this.loadParentOptions();

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
      this.getDetail();
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

  private getDetail() {
    this.menuService.getById(this.data.id!).subscribe({
      next: ({ data }) => {
        this.currentData = data;
        this.form.patchValue(this.normalizeFormData(data));
        this.applyParentOptions();
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được chi tiết chức năng',
          'Thất bại'
        );
      },
    });
  }

  private loadParentOptions() {
    this.menuService.getOptions().subscribe({
      next: ({ data }) => {
        this.parentOptions = data ?? [];
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
      this.form.getRawValue()?.[MENU_KEY.PARENT_ID] ?? this.currentData?.parentId;

    this.findFormControl(this.$formItem, MENU_KEY.PARENT_ID).options = [
      {
        value: '',
        label: 'Không có menu cha',
      },
      ...this.parentOptions
        .filter((item) => item.id !== currentId)
        .map((item) => ({
          value: item.id,
          label:
            item.parentId != null
              ? `${item.name} (${item.code})`
              : `${item.name} (${item.code}) - menu gốc`,
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
    return {
      ...data,
      [MENU_KEY.PARENT_ID]: data[MENU_KEY.PARENT_ID] ?? '',
      [MENU_KEY.ICON]: data[MENU_KEY.ICON] ?? null,
      [MENU_KEY.ORDINAL]: data[MENU_KEY.ORDINAL] ?? 1,
      [MENU_KEY.URL]: data[MENU_KEY.URL] ?? '',
    };
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
