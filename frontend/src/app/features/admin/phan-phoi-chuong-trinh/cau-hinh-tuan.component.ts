import { Component, Injector, TemplateRef, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { distinctUntilChanged, takeUntil } from 'rxjs';

import { AppTableComponent } from '@components/app-table/app-table.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TYPE_FORM } from '@constant/constant';
import { ComponentBaseAbstract } from '@layout';
import { DATE_CONTROL, FormType } from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { PermissionCheckService } from '@service';

import { HocKyResponse, HOC_KY_KEY } from '@app/model/admin/hoc-ky.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import {
  WEEK_CONFIG_FILTER_FORM,
  WEEK_CONFIG_KEY,
  WeekConfigItemPayload,
  WeekConfigResponse,
} from '@app/model/admin/week-config.model';
import { HocKyService } from '@app/service/admin/hoc-ky.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { WeekConfigService } from '@app/service/admin/week-config.service';

@Component({
  selector: 'cau-hinh-tuan',
  standalone: true,
  templateUrl: './cau-hinh-tuan.component.html',
  styleUrls: ['./cau-hinh-tuan.component.scss'],
  imports: [
    AppTableComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class CauHinhTuanComponent extends ComponentBaseAbstract {
  @ViewChild('fromDateTpl', { static: true })
  fromDateTpl!: TemplateRef<unknown>;
  @ViewChild('toDateTpl', { static: true })
  toDateTpl!: TemplateRef<unknown>;

  override readonly TYPE_FORM = TYPE_FORM;
  readonly menuCode = 'CURRICULUM_DISTRIBUTION';
  readonly key = WEEK_CONFIG_KEY;
  readonly tableConfig = {
    hasFilterPanel: true,
    showPaginator: false,
  };

  columns: MtxGridColumn[] = [];
  $formItem: FormType[] = structuredClone(WEEK_CONFIG_FILTER_FORM);
  dataSource: WeekConfigResponse[] = [];
  private rowForms = new Map<string, FormGroup>();

  readonly fromDateControlItem: FormType = DATE_CONTROL({
    controlName: WEEK_CONFIG_KEY.START_DATE,
    placeholder: 'Từ ngày',
    required: false,
    dateType: 'date',
    showLabel: false,
  });

  readonly toDateControlItem: FormType = DATE_CONTROL({
    controlName: WEEK_CONFIG_KEY.END_DATE,
    placeholder: 'Đến ngày',
    required: false,
    dateType: 'date',
    showLabel: false,
  });

  get canConfig(): boolean {
    return this.permissionCheckService.canConfig(this.menuCode);
  }

  constructor(
    protected override injector: Injector,
    private readonly weekConfigService: WeekConfigService,
    private readonly namHocService: NamHocService,
    private readonly hocKyService: HocKyService,
    private readonly permissionCheckService: PermissionCheckService
  ) {
    super(injector);
    this.pageSize = 200;
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.initializeColumns();
    this.watchSchoolYearChanges();
    this.loadSchoolYearOptions();
  }

  filterData(pageChangeEvent?: TableQueryEvent): void {
    this.pageIndex = pageChangeEvent?.pageIndex ?? 0;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    const formValues = this.form.getRawValue();
    const schoolYearId = formValues[this.key.SCHOOL_YEAR_ID] as ID_TYPE;
    const semesterId = formValues[this.key.SEMESTER_ID] as ID_TYPE;

    if (schoolYearId == null || schoolYearId === '') {
      this.dataSource = [];
      this.dataSourceTotal = 0;
      return;
    }

    this.weekConfigService
      .getList({
        schoolYearId,
        semesterId: semesterId || undefined,
      })
      .subscribe({
        next: ({ data }) => {
          const items = Array.isArray(data)
            ? data
            : data?.items || data?.data || [];
          this.dataSource = items;
          this.initRowForms();
          this.dataSourceTotal = Array.isArray(data)
            ? data.length
            : data?.recordTotal || items.length;
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được cấu hình tuần',
            'Thất bại'
          );
        },
      });
  }

  resetFilter(): void {
    this.form.reset();
    this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];
    this.dataSource = [];
    this.rowForms.clear();
    this.dataSourceTotal = 0;
    this.appTableComponent.resetQuery();
  }

  getRowForm(rowData: WeekConfigResponse): FormGroup {
    const rowKey = this.getRowKey(rowData);
    const existed = this.rowForms.get(rowKey);
    if (existed) {
      return existed;
    }

    const form = this.itemControl.toFormGroup([
      this.fromDateControlItem,
      this.toDateControlItem,
    ]);

    form.patchValue(
      {
        [this.key.START_DATE]: this.toFormDate(rowData[this.key.START_DATE]),
        [this.key.END_DATE]: this.toFormDate(rowData[this.key.END_DATE]),
      },
      { emitEvent: false }
    );

    if (!this.canConfig) {
      form.disable({ emitEvent: false });
    }

    this.rowForms.set(rowKey, form);
    return form;
  }

  generateWeekConfig(): void {
    const formValues = this.form.getRawValue();
    const schoolYearId = formValues[this.key.SCHOOL_YEAR_ID] as ID_TYPE;
    const semesterId = formValues[this.key.SEMESTER_ID] as ID_TYPE;

    if (schoolYearId == null || schoolYearId === '') {
      this.toastr.error('Vui lòng chọn năm học', 'Thất bại');
      return;
    }

    if (semesterId == null || semesterId === '') {
      this.toastr.error('Vui lòng chọn học kỳ để sinh tuần', 'Thất bại');
      return;
    }

    this.weekConfigService
      .generate({
        schoolYearId,
        semesterId,
      })
      .subscribe({
        next: () => {
          this.toastr.success('Sinh cấu hình tuần thành công', 'Thành công');
          this.filterData();
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Sinh cấu hình tuần thất bại',
            'Thất bại'
          );
        },
      });
  }

  saveBulkUpdate(): void {
    if (!this.dataSource.length) {
      this.toastr.error('Không có dữ liệu để lưu', 'Thất bại');
      return;
    }

    for (const row of this.dataSource) {
      const rowForm = this.getRowForm(row);
      const rowValue = rowForm.getRawValue();
      const startDate = this.toApiDate(rowValue[this.key.START_DATE]);
      const endDate = this.toApiDate(rowValue[this.key.END_DATE]);

      if (!startDate || !endDate) {
        this.toastr.error(
          `Tuần ${row[this.key.WEEK_NUMBER] ?? '--'} chưa nhập đủ từ ngày/đến ngày`,
          'Thất bại'
        );
        return;
      }

      if (startDate > endDate) {
        this.toastr.error(
          `Tuần ${row[this.key.WEEK_NUMBER] ?? '--'} có từ ngày lớn hơn đến ngày`,
          'Thất bại'
        );
        return;
      }
    }

    const items: WeekConfigItemPayload[] = this.dataSource.map((item) => ({
      id: item[this.key.ID],
      weekNumber: Number(item[this.key.WEEK_NUMBER] ?? 0),
      startDate: this.toApiDate(
        this.getRowForm(item).getRawValue()[this.key.START_DATE]
      ),
      endDate: this.toApiDate(
        this.getRowForm(item).getRawValue()[this.key.END_DATE]
      ),
    }));

    this.weekConfigService.bulkUpdate({ items }).subscribe({
      next: () => {
        this.toastr.success(
          'Lưu danh sách cấu hình tuần thành công',
          'Thành công'
        );
        this.filterData();
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Lưu danh sách cấu hình tuần thất bại',
          'Thất bại'
        );
      },
    });
  }

  cancelBulkUpdate(): void {
    this.filterData();
  }

  deleteBySemester(): void {
    const semesterId = this.form.getRawValue()[this.key.SEMESTER_ID] as ID_TYPE;
    if (semesterId == null || semesterId === '') {
      this.toastr.error(
        'Vui lòng chọn học kỳ để xóa cấu hình tuần',
        'Thất bại'
      );
      return;
    }

    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message:
          'Bạn có chắc chắn muốn xóa toàn bộ cấu hình tuần của học kỳ đã chọn không?',
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.weekConfigService.deleteBySemester(semesterId).subscribe({
          next: () => {
            this.toastr.success('Xóa cấu hình tuần thành công', 'Thành công');
            this.filterData();
          },
          error: (error) => {
            this.toastr.error(
              error?.error?.userMessage ??
                error?.error?.message ??
                'Xóa cấu hình tuần thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  goBack(): void {
    this.router.navigate([
      '/',
      this.navigatorEndpoint.ADMIN.BASE_PATH,
      this.navigatorEndpoint.ADMIN.CURRICULUM_DISTRIBUTION.BASE_PATH,
    ]);
  }

  private loadSchoolYearOptions(): void {
    this.namHocService.getOptions().subscribe({
      next: ({ data }) => {
        this.findFormControl(this.$formItem, this.key.SCHOOL_YEAR_ID).options =
          (data ?? []).map((item: NamHocOptionResponse) => ({
            value: item.id,
            label: item.name,
          }));

        this.loadDefaultSchoolYear();
      },
      error: () => {
        this.loadDefaultSchoolYear();
      },
    });
  }

  private loadDefaultSchoolYear(): void {
    this.namHocService.getCurrent().subscribe({
      next: ({ data }) => {
        if (!data?.id) {
          return;
        }

        this.form.patchValue({
          [this.key.SCHOOL_YEAR_ID]: data.id,
        });
      },
      error: () => {
        this.filterData();
      },
    });
  }

  private loadSemesterOptions(schoolYearId: ID_TYPE): void {
    this.hocKyService.filter({ schoolYearId }).subscribe({
      next: ({ data }) => {
        const items = Array.isArray(data) ? data : data?.items || [];
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = (
          items ?? []
        ).map((item: HocKyResponse) => ({
          value: item[HOC_KY_KEY.ID],
          label: item[HOC_KY_KEY.NAME] ?? '',
        }));

        this.filterData();
      },
      error: () => {
        this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options = [];
        this.rowForms.clear();
        this.filterData();
      },
    });
  }

  private watchSchoolYearChanges(): void {
    this.form
      .get(this.key.SCHOOL_YEAR_ID)
      ?.valueChanges.pipe(distinctUntilChanged(), takeUntil(this.ngUnsubscribe))
      .subscribe((schoolYearId) => {
        this.form.patchValue(
          {
            [this.key.SEMESTER_ID]: null,
          },
          { emitEvent: false }
        );

        if (schoolYearId == null || schoolYearId === '') {
          this.findFormControl(this.$formItem, this.key.SEMESTER_ID).options =
            [];
          this.dataSource = [];
          this.rowForms.clear();
          this.dataSourceTotal = 0;
          return;
        }

        this.loadSemesterOptions(schoolYearId as ID_TYPE);
      });
  }

  private initializeColumns(): void {
    const baseColumns: MtxGridColumn[] = [
      {
        header: 'STT',
        class: 'text-center',
        field: COMMON_TABLE_KEY.STT,
      },
      {
        header: 'Tuần',
        field: this.key.WEEK_NUMBER,
        class: 'text-center',
      },
      {
        header: 'Từ ngày',
        field: this.key.START_DATE,
        cellTemplate: this.fromDateTpl,
      },
      {
        header: 'Đến ngày',
        field: this.key.END_DATE,
        cellTemplate: this.toDateTpl,
      },
    ];

    this.columns = baseColumns;
  }

  private toApiDate(value: unknown): string {
    if (!value) return '';
    if (value instanceof Date) {
      return value.toISOString().slice(0, 10);
    }

    return String(value).slice(0, 10);
  }

  private toFormDate(value?: string): string | null {
    if (!value) return null;
    return value.includes('T') ? value : `${value}T00:00:00`;
  }

  private initRowForms(): void {
    this.rowForms.clear();
    this.dataSource.forEach((row) => {
      this.getRowForm(row);
    });
  }

  private getRowKey(rowData: WeekConfigResponse): string {
    const id = rowData[this.key.ID];
    return id == null ? `${rowData[this.key.WEEK_NUMBER] ?? ''}` : String(id);
  }
}
