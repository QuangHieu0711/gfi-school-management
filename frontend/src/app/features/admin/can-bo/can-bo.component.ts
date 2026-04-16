import { CommonModule } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { debounceTime, takeUntil } from 'rxjs';

import { AppPaginatorComponent } from '@components/app-paginator/app-paginator.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { FormType, IOptions } from '@model/form-control.model';
import { TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { PermissionCheckService } from '@service';

import {
  CAN_BO_DETAIL_FALLBACK,
  CAN_BO_FILTER_FORM,
  CAN_BO_GENDER_OPTIONS,
  CAN_BO_KEY,
  CAN_BO_STATUS_OPTIONS,
  CanBoFilterRequest,
  CanBoResponse,
} from '@app/model/admin/can-bo.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { CanBoService } from '@app/service/admin/can-bo.service';

@Component({
  selector: 'can-bo',
  standalone: true,
  templateUrl: './can-bo.component.html',
  styleUrls: ['./can-bo.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppPaginatorComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class CanBoComponent extends ComponentBaseAbstract {
  readonly menuCode = 'STAFF_PROFILE';
  dataSource: CanBoResponse[] = [];
  key = CAN_BO_KEY;
  $formItem: FormType[] = CAN_BO_FILTER_FORM.map((item) => ({
    ...item,
    showLabel: false,
  }));
  readonly statusOptions: IOptions[] = CAN_BO_STATUS_OPTIONS;
  readonly genderOptions: IOptions[] = CAN_BO_GENDER_OPTIONS;
  unitOptions: IOptions[] = [];

  get canEdit(): boolean {
    return this.permissionCheckService.canEdit(this.menuCode);
  }

  get canDelete(): boolean {
    return this.permissionCheckService.canDelete(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly canBoService: CanBoService,
    private readonly donViService: DonViService,
    private readonly routerService: Router,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.bindInlineFilter();
    this.loadOptions();
    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent): void {
    this.pageIndex = pageChangeEvent?.pageIndex ?? this.pageIndex;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.canBoService
      .filter(this.buildFilterPayload(pageChangeEvent))
      .subscribe({
        next: ({ data }) => {
          this.dataSource = (data.items ?? []).map((item) =>
            this.normalizeRow(item)
          );
          this.dataSourceTotal = data.recordTotal ?? 0;

          if (!this.dataSource.length && !this.hasAnyFilter()) {
            this.dataSource = [this.normalizeRow(CAN_BO_DETAIL_FALLBACK)];
            this.dataSourceTotal = 1;
          }
        },
        error: () => {
          this.dataSource = [this.normalizeRow(CAN_BO_DETAIL_FALLBACK)];
          this.dataSourceTotal = 1;
        },
      });
  }

  submitInlineFilter(): void {
    this.pageIndex = 0;
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

  openDetail(staff: CanBoResponse): void {
    this.routerService.navigate(
      [
        '/',
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
        PATH.CHI_TIET,
        staff[this.key.ID],
      ],
      { state: { staff } }
    );
  }

  openEdit(staff: CanBoResponse): void {
    this.routerService.navigate(
      [
        '/',
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
        PATH.CAP_NHAT,
        staff[this.key.ID],
      ],
      { state: { staff } }
    );
  }

  updateStatus(_staff: CanBoResponse): void {
    this.toastr.info(
      'Chức năng cập nhật trạng thái đang được hoàn thiện',
      'Thông báo'
    );
  }

  assignTeaching(_staff: CanBoResponse): void {
    this.toastr.info(
      'Chức năng phân công giảng dạy đang được hoàn thiện',
      'Thông báo'
    );
  }

  deleteStaff(staff: CanBoResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa cán bộ ${staff.fullName ?? ''} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.canBoService.delete(staff[this.key.ID]).subscribe({
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

  getStatusLabel(status?: string): string {
    const normalized = `${status ?? ''}`.toUpperCase();
    return (
      this.statusOptions.find(
        (item) => `${item.value}`.toUpperCase() === normalized
      )?.label ??
      status ??
      'Chưa cập nhật'
    );
  }

  getStatusClass(status?: string): string {
    const normalized = `${status ?? ''}`.toUpperCase();
    if (normalized === 'ACTIVE') return 'status-pill active';
    if (normalized === 'INACTIVE') return 'status-pill inactive';
    return 'status-pill';
  }

  private bindInlineFilter(): void {
    this.form.valueChanges
      .pipe(debounceTime(300), takeUntil(this.ngUnsubscribe))
      .subscribe(() => {
        this.pageIndex = 0;
        this.filterData({
          pageIndex: 0,
          pageSize: this.pageSize,
        });
      });
  }

  private loadOptions(): void {
    this.donViService.getCreateUserUnitOptions().subscribe(({ data }) => {
      this.unitOptions = (data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));
      this.findFormControl(this.$formItem, this.key.UNIT_ID).options =
        this.unitOptions;
    });
  }

  private buildFilterPayload(
    pageChangeEvent?: TableQueryEvent
  ): CanBoFilterRequest {
    const value = this.form.getRawValue();

    return {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? this.pageIndex) + 1,
      filter: {
        staffCode: value[this.key.STAFF_CODE] ?? undefined,
        fullName: value[this.key.FULL_NAME] ?? undefined,
        unitId: value[this.key.UNIT_ID] ?? undefined,
        status: value[this.key.STATUS] ?? undefined,
        gender: value[this.key.GENDER] ?? undefined,
        phone: value[this.key.PHONE] ?? undefined,
        email: value[this.key.EMAIL] ?? undefined,
        dateOfBirth: this.normalizeDateValue(value[this.key.DATE_OF_BIRTH]),
      },
    };
  }

  private normalizeDateValue(value: unknown): string | undefined {
    if (typeof value !== 'string' || !value) return undefined;
    if (value.includes('/')) {
      const [day, month, year] = value.split('/');
      if (day && month && year) {
        return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
      }
    }
    return value.slice(0, 10);
  }

  private hasAnyFilter(): boolean {
    return Object.values(this.form.getRawValue()).some(
      (value) => value !== null && value !== undefined && value !== ''
    );
  }

  private normalizeRow(item: CanBoResponse): CanBoResponse {
    return {
      ...item,
      unitName: item.unitName ?? this.getUnitLabel(item.unitId),
      aliasName: item.aliasName || this.extractAliasName(item.fullName),
      dateOfBirth: this.formatDate(item.dateOfBirth),
      gender: this.formatGender(item.gender),
    };
  }

  private getUnitLabel(unitId?: string | number): string {
    return this.unitOptions.find((item) => item.value === unitId)?.label ?? '';
  }

  private extractAliasName(fullName?: string): string {
    if (!fullName) return '';
    const chunks = fullName.trim().split(/\s+/);
    return chunks[chunks.length - 1] ?? '';
  }

  private formatDate(value?: string): string {
    if (!value) return '';
    const raw = value.slice(0, 10);
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  private formatGender(value?: string): string {
    const normalized = `${value ?? ''}`.trim().toUpperCase();
    if (!normalized) return '';
    if (normalized === 'MALE' || normalized === 'NAM' || normalized === '0') {
      return 'Nam';
    }
    if (
      normalized === 'FEMALE' ||
      normalized === 'NU' ||
      normalized === 'NỮ' ||
      normalized === '1'
    ) {
      return 'Nữ';
    }
    return `${value ?? ''}`;
  }
}
