/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { debounceTime, takeUntil } from 'rxjs';

import { AppPaginatorComponent } from '@components/app-paginator/app-paginator.component';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { PATH, NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { TableQueryEvent } from '@model/table.model';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { FormType, IOptions } from '@model/form-control.model';
import { defaultExportFileName, saveBlobAsFile } from '@utils/file-util';

import {
  HOC_SINH_DETAIL_FALLBACK,
  HOC_SINH_FILTER_FORM,
  HOC_SINH_KEY,
  HocSinhExportRequest,
  HocSinhFilterRequest,
  HocSinhResponse,
} from '@app/model/admin/hoc-sinh.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { KhoiService } from '@app/service/admin/khoi.service';
import { LopService } from '@app/service/admin/lop.service';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { AuthService, PermissionCheckService } from '@service';
import { DialogImportHocSinhComponent } from './dialog-import/dialog-import.component';
import { DialogTransferClassComponent } from './dialog-transfer-class/dialog-transfer-class.component';
import { DialogExportHocBaComponent } from './dialog-export-hoc-ba/dialog-export-hoc-ba.component';

@Component({
  selector: 'hoc-sinh',
  standalone: true,
  templateUrl: './hoc-sinh.component.html',
  styleUrls: ['./hoc-sinh.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppPaginatorComponent,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class HocSinhComponent extends ComponentBaseAbstract {
  readonly menuCode = 'STUDENT_PROFILE';
  dataSource: HocSinhResponse[] = [];
  selectedStudentIds = new Set<string>();
  key = HOC_SINH_KEY;
  $formItem: FormType[] = HOC_SINH_FILTER_FORM.map((item) => ({
    ...item,
    showLabel: false,
  }));
  readonly statusOptions: IOptions[] = [
    { value: 0, label: 'Đang học' },
    { value: 1, label: 'Đã chuyển trường' },
    { value: 2, label: 'Tạm nghỉ' },
    { value: 3, label: 'Thôi học' },
  ];
  readonly genderOptions: IOptions[] = [
    { value: 'Nam', label: 'Nam' },
    { value: 'Nu', label: 'Nữ' },
  ];
  classOptions: IOptions[] = [];
  gradeOptions: IOptions[] = [];
  unitOptions: IOptions[] = [];

  get canAdd(): boolean {
    return this.permissionCheckService.canAdd(this.menuCode);
  }

  get canEdit(): boolean {
    return this.permissionCheckService.canEdit(this.menuCode);
  }

  get canDelete(): boolean {
    return this.permissionCheckService.canDelete(this.menuCode);
  }

  get canDownload(): boolean {
    return this.permissionCheckService.canDownload(this.menuCode);
  }

  get selectedStudents(): HocSinhResponse[] {
    return this.dataSource.filter((item) =>
      this.selectedStudentIds.has(this.toSelectionKey(item.id))
    );
  }

  get hasSelectedStudents(): boolean {
    return this.selectedStudentIds.size > 0;
  }

  constructor(
    protected override injector: Injector,
    private readonly hocSinhService: HocSinhService,
    private readonly donViService: DonViService,
    private readonly lopService: LopService,
    private readonly khoiService: KhoiService,
    private readonly routerService: Router,
    private readonly permissionCheckService: PermissionCheckService,
    private readonly authService: AuthService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.itemControl.toFormGroup(this.$formItem);
    this.findFormControl(this.$formItem, this.key.FULL_NAME).placeholder =
      'Họ và tên';
    this.findFormControl(this.$formItem, this.key.FIRST_NAME).placeholder =
      'Tên';
    this.findFormControl(this.$formItem, this.key.UNIT_ID).placeholder =
      'Đơn vị';
    this.findFormControl(this.$formItem, this.key.STUDENT_STATUS).options =
      this.statusOptions;
    this.findFormControl(this.$formItem, this.key.STUDENT_STATUS).placeholder =
      'Trạng thái';
    this.findFormControl(this.$formItem, this.key.CLASS_ID).placeholder =
      'Tên lớp';
    this.findFormControl(this.$formItem, this.key.MOE_CODE).placeholder =
      'Mã MOET';
    this.findFormControl(this.$formItem, this.key.GRADE_LEVEL_ID).placeholder =
      'Khối';
    this.findFormControl(this.$formItem, this.key.DATE_OF_BIRTH).placeholder =
      'Ngày sinh';
    this.findFormControl(this.$formItem, this.key.GENDER).options =
      this.genderOptions;
    this.findFormControl(this.$formItem, this.key.GENDER).placeholder =
      'Giới tính';
    this.findFormControl(this.$formItem, this.key.STUDENT_CODE).placeholder =
      'Mã học sinh';
    this.findFormControl(
      this.$formItem,
      this.key.OTHER_SYSTEM_CODE
    ).placeholder = 'Mã hệ thống khác';
    this.findFormControl(this.$formItem, this.key.FATHER_PHONE).placeholder =
      'Số điện thoại bố';
    this.findFormControl(this.$formItem, this.key.MOTHER_PHONE).placeholder =
      'Số điện thoại mẹ';
    this.findFormControl(
      this.$formItem,
      this.key.PERMANENT_PROVINCE_NAME
    ).placeholder = 'Tỉnh/TP thường trú';
    this.findFormControl(
      this.$formItem,
      this.key.PERMANENT_WARD_NAME
    ).placeholder = 'Xã/phường thường trú';
    this.bindInlineFilter();
    this.loadOptions();
    this.filterData({
      pageIndex: 0,
      pageSize: this.pageSize,
    });
  }

  filterData(pageChangeEvent?: TableQueryEvent) {
    this.pageIndex = pageChangeEvent?.pageIndex ?? this.pageIndex;
    this.pageSize = pageChangeEvent?.pageSize ?? this.pageSize;

    this.hocSinhService
      .filter(this.buildFilterPayload(pageChangeEvent))
      .subscribe({
        next: ({ data }) => {
          this.selectedStudentIds.clear();
          this.dataSource = (data.items ?? []).map((item) =>
            this.normalizeRow(item)
          );
          this.dataSourceTotal = data.recordTotal ?? 0;

          if (!this.dataSource.length && !this.hasAnyFilter()) {
            this.dataSource = [this.normalizeRow(HOC_SINH_DETAIL_FALLBACK)];
            this.dataSourceTotal = 1;
          }
        },
        error: () => {
          this.selectedStudentIds.clear();
          this.dataSource = [this.normalizeRow(HOC_SINH_DETAIL_FALLBACK)];
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

  openDetail(student: HocSinhResponse): void {
    this.routerService.navigate(
      [
        '/',
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
        PATH.CHI_TIET,
        student[this.key.ID],
      ],
      { state: { student: this.normalizeRow(student) } }
    );
  }

  openEdit(student: HocSinhResponse): void {
    this.routerService.navigate([
      '/',
      NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
      NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
      PATH.CAP_NHAT,
      student[this.key.ID],
    ]);
  }

  deleteStudent(student: HocSinhResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa học sinh ${student.fullName ?? ''} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.hocSinhService.delete(student[this.key.ID]).subscribe({
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

  openCreate(): void {
    this.routerService.navigate([
      '/',
      NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
      NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
      PATH.TAO_MOI,
    ]);
  }

  import(): void {
    if (!this.canAdd) {
      this.toastr.warning('Bạn không có quyền kết nạp dữ liệu', 'Cảnh báo');
      return;
    }

    this.dialog.componentDialog(
      DialogImportHocSinhComponent,
      {
        width: '900px',
      },
      (result) => {
        if (result) {
          this.pageIndex = 0;
          this.filterData({
            pageIndex: 0,
            pageSize: this.pageSize,
          });
        }
      }
    );
  }

  openTransferClass(): void {
    if (!this.canEdit) {
      this.toastr.warning('Bạn không có quyền chuyển lớp', 'Cảnh báo');
      return;
    }

    const students = this.selectedStudents;
    if (!students.length) {
      this.toastr.warning('Vui lòng chọn ít nhất một học sinh', 'Cảnh báo');
      return;
    }

    const unitId = students[0]?.unitId;
    if (!unitId) {
      this.toastr.warning('Không xác định được đơn vị của học sinh đã chọn', 'Cảnh báo');
      return;
    }

    const hasMixedUnit = students.some(
      (student) => `${student.unitId ?? ''}` !== `${unitId}`
    );
    if (hasMixedUnit) {
      this.toastr.warning(
        'Chỉ có thể chuyển lớp cho các học sinh cùng đơn vị',
        'Cảnh báo'
      );
      return;
    }

    this.dialog.componentDialog(
      DialogTransferClassComponent,
      {
        width: '720px',
        data: {
          unitId,
          students,
        },
      },
      (result) => {
        if (!result) return;
        this.selectedStudentIds.clear();
        this.filterData({
          pageIndex: this.pageIndex,
          pageSize: this.pageSize,
        });
      }
    );
  }

  openExportHocBa(): void {
    const students = this.selectedStudents;
    if (!students.length) {
      this.toastr.warning(
        'Vui lòng tích chọn ít nhất một học sinh để xuất học bạ',
        'Cảnh báo'
      );
      return;
    }

    this.dialog.componentDialog(
      DialogExportHocBaComponent,
      {
        width: '520px',
        data: { students },
      },
      () => {}
    );
  }

  exportExcel(): void {
    if (!this.canDownload) {
      this.toastr.warning('Bạn không có quyền tải xuống', 'Cảnh báo');
      return;
    }

    this.exportFile('EXCEL');
  }

  exportPdf(): void {
    if (!this.canDownload) {
      this.toastr.warning('Bạn không có quyền tải xuống', 'Cảnh báo');
      return;
    }

    this.exportFile('PDF');
  }

  getStatusLabel(status?: number): string {
    return (
      this.statusOptions.find((item) => item.value === status)?.label ??
      'Chưa cập nhật'
    );
  }

  isAllCurrentPageSelected(): boolean {
    return (
      this.dataSource.length > 0 &&
      this.dataSource.every((item) =>
        this.selectedStudentIds.has(this.toSelectionKey(item.id))
      )
    );
  }

  isSomeCurrentPageSelected(): boolean {
    const selectedCount = this.dataSource.filter((item) =>
      this.selectedStudentIds.has(this.toSelectionKey(item.id))
    ).length;
    return selectedCount > 0 && selectedCount < this.dataSource.length;
  }

  toggleSelectAllCurrentPage(checked: boolean): void {
    if (checked) {
      this.dataSource.forEach((item) =>
        this.selectedStudentIds.add(this.toSelectionKey(item.id))
      );
      return;
    }

    this.dataSource.forEach((item) =>
      this.selectedStudentIds.delete(this.toSelectionKey(item.id))
    );
  }

  toggleStudentSelection(student: HocSinhResponse, checked: boolean): void {
    const key = this.toSelectionKey(student.id);
    if (checked) {
      this.selectedStudentIds.add(key);
      return;
    }
    this.selectedStudentIds.delete(key);
  }

  isStudentSelected(student: HocSinhResponse): boolean {
    return this.selectedStudentIds.has(this.toSelectionKey(student.id));
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

    this.lopService.getOptions().subscribe(({ data }) => {
      this.classOptions = (data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));
      this.findFormControl(this.$formItem, this.key.CLASS_ID).options =
        this.classOptions;
    });

    this.khoiService.getOptions().subscribe(({ data }) => {
      this.gradeOptions = (data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));
      this.findFormControl(this.$formItem, this.key.GRADE_LEVEL_ID).options =
        this.gradeOptions;
    });
  }

  private buildFilterPayload(
    pageChangeEvent?: TableQueryEvent
  ): HocSinhFilterRequest {
    const value = this.form.getRawValue();

    return {
      pageSize: pageChangeEvent?.pageSize ?? this.pageSize,
      pageNow: (pageChangeEvent?.pageIndex ?? this.pageIndex) + 1,
      filter: {
        fullName: value[this.key.FULL_NAME] ?? undefined,
        firstName: value[this.key.FIRST_NAME] ?? undefined,
        unitId: value[this.key.UNIT_ID] ?? this.getCurrentUnitId() ?? undefined,
        studentStatus: value[this.key.STUDENT_STATUS] ?? undefined,
        classId: value[this.key.CLASS_ID] ?? undefined,
        moeCode: value[this.key.MOE_CODE] ?? undefined,
        gradeLevelId: value[this.key.GRADE_LEVEL_ID] ?? undefined,
        dateOfBirth: this.normalizeDateValue(value[this.key.DATE_OF_BIRTH]),
        gender: value[this.key.GENDER] ?? undefined,
        studentCode: value[this.key.STUDENT_CODE] ?? undefined,
        otherSystemCode: value[this.key.OTHER_SYSTEM_CODE] ?? undefined,
        fatherPhone: value[this.key.FATHER_PHONE] ?? undefined,
        motherPhone: value[this.key.MOTHER_PHONE] ?? undefined,
        permanentProvinceName:
          value[this.key.PERMANENT_PROVINCE_NAME] ?? undefined,
        permanentWardName: value[this.key.PERMANENT_WARD_NAME] ?? undefined,
      },
    };
  }

  private exportFile(exportType: 'PDF' | 'EXCEL'): void {
    const payload: HocSinhExportRequest = {
      ...this.buildFilterPayload({
        pageIndex: this.pageIndex,
        pageSize: this.pageSize,
      }),
      exportType,
    };

    this.hocSinhService.export(payload).subscribe({
      next: (res: any) => {
        this.toastr.removeToastr();

        const blob = this.extractBlob(res);
        if (!blob) {
          this.toastr.error(
            `Xuất ${exportType} thất bại: Dữ liệu không hợp lệ`,
            'Lỗi'
          );
          return;
        }

        const ext = exportType === 'PDF' ? 'pdf' : 'xlsx';
        const fallbackName = defaultExportFileName('hoc-sinh', ext);
        const disposition = this.getHeader(res, 'content-disposition');
        const fileName = this.getFileNameFromDisposition(
          disposition,
          fallbackName
        );

        saveBlobAsFile(blob, fileName);
        this.toastr.success(
          `Tải xuống ${exportType} thành công`,
          `Xuất ${exportType}`
        );
      },
      error: () => {
        this.toastr.removeToastr();
        this.toastr.error(`Xuất ${exportType} thất bại`, 'Lỗi');
      },
    });
  }

  private getCurrentUnitId(): string | number | undefined {
    const unitId = this.authService.currentUser?.unit?.id;
    return unitId == null || unitId === '' ? undefined : unitId;
  }

  private extractBlob(res: any): Blob | null {
    if (res instanceof Blob) return res;
    if (res?.body instanceof Blob) return res.body;
    if (res?.data instanceof Blob) return res.data;
    return null;
  }

  private getHeader(res: any, headerName: string): string | null {
    if (res?.headers?.get) return res.headers.get(headerName);

    const headers = res?.headers;
    if (headers && typeof headers === 'object') {
      const key = headerName.toLowerCase();
      return headers[headerName] ?? headers[key] ?? null;
    }

    return null;
  }

  private getFileNameFromDisposition(
    disposition: string | null,
    fallbackName: string
  ): string {
    if (!disposition) return fallbackName;

    const utf8StarMatch = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
    if (utf8StarMatch?.[1]) {
      try {
        return decodeURIComponent(utf8StarMatch[1].trim());
      } catch {
        return utf8StarMatch[1].trim();
      }
    }

    const filenameMatch = disposition.match(/filename\s*=\s*"?([^";]+)"?/i);
    if (!filenameMatch?.[1]) return fallbackName;

    return this.decodeMimeFileName(filenameMatch[1].trim()) || fallbackName;
  }

  private decodeMimeFileName(value: string): string {
    if (!value) return value;

    const match = value.match(/^=\?([^?]+)\?([BQ])\?([^?]+)\?=$/i);
    if (!match) return value;

    const [, charset, encoding, encodedText] = match;
    if (!/^utf-8$/i.test(charset)) return value;

    try {
      if (encoding.toUpperCase() === 'B') {
        const percentEncoded = Array.from(atob(encodedText))
          .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
          .join('');
        return decodeURIComponent(percentEncoded);
      }

      return decodeURIComponent(
        encodedText.replace(/_/g, ' ').replace(/=([0-9A-F]{2})/gi, '%$1')
      );
    } catch {
      return value;
    }
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

  private normalizeRow(item: HocSinhResponse): HocSinhResponse {
    const father = item.guardians?.find((guardian) =>
      `${guardian.guardianType}`.toUpperCase().includes('FATHER')
    );
    const mother = item.guardians?.find((guardian) =>
      `${guardian.guardianType}`.toUpperCase().includes('MOTHER')
    );
    const permanentAddress = item.addresses?.find((address) =>
      `${address.addressType}`.toLowerCase().includes('thuong')
    );

    return {
      ...item,
      firstName: item.firstName || this.extractFirstName(item.fullName),
      classId: item.classId ?? item.enrollment?.classId,
      className: item.className ?? item.enrollment?.className ?? '--',
      gradeLevelId: item.gradeLevelId ?? item.enrollment?.gradeLevelId,
      gradeLevelName:
        item.gradeLevelName ?? item.enrollment?.gradeLevelName ?? '--',
      fatherPhone: item.fatherPhone ?? father?.phone ?? '',
      motherPhone: item.motherPhone ?? mother?.phone ?? '',
      permanentProvinceName:
        item.permanentProvinceName ?? permanentAddress?.provinceName ?? '--',
      permanentWardName:
        item.permanentWardName ?? permanentAddress?.wardName ?? '--',
      dateOfBirth: this.formatDate(item.dateOfBirth),
      gender: this.formatGender(item.gender),
    };
  }

  private extractFirstName(fullName?: string): string {
    if (!fullName) return '--';
    const chunks = fullName.trim().split(/\s+/);
    return chunks[chunks.length - 1] ?? '--';
  }

  private formatDate(value?: string): string {
    if (!value) return '--';
    const raw = value.slice(0, 10);
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  private formatGender(value?: string | number): string {
    if (value === 0 || value === '0') return 'Nam';
    if (value === 1 || value === '1') return 'Nữ';
    if (value === 'Nam' || value === 'Nu') {
      return value === 'Nu' ? 'Nữ' : value;
    }
    return value ? `${value}` : '';
  }

  private toSelectionKey(value: string | number | undefined): string {
    return `${value ?? ''}`;
  }
}
