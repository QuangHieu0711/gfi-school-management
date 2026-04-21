import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { filter, firstValueFrom, takeUntil } from 'rxjs';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { environment } from '@env/environment';
import { ComponentBaseAbstract } from '@layout';
import {
  DATE_CONTROL,
  FormType,
  IOptions,
  SELECT_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
  CHECKBOX_CONTROL,
} from '@model/form-control.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import { CanBoService } from '@app/service/admin/can-bo.service';
import { DonViService } from '@app/service/admin/don-vi.service';
import { StaffTrainingService } from '@app/service/admin/dao-tao-can-bo.service';
import { StaffForeignLanguageService } from '@app/service/admin/thong-tin-ngoai-ngu-can-bo.service';
import { StaffJobHistoryService } from '@app/service/admin/qua-trinh-cong-tac.service';
import { KhoiService } from '@app/service/admin/khoi.service';
import { DAN_TOC_OPTIONS } from '@app/model/admin/dan-toc.model';
import {
  CAN_BO_GENDER_OPTIONS,
  CAN_BO_PROFILE_FALLBACK,
  CAN_BO_STATUS_OPTIONS,
  CanBoDetailResponse,
  CanBoFormRequest,
} from '@app/model/admin/can-bo.model';
import {
  STAFF_TRAINING_KEY,
  StaffTrainingResponse,
} from '@app/model/admin/dao-tao-can-bo.model';
import {
  STAFF_FOREIGN_LANGUAGE_KEY,
  StaffForeignLanguageResponse,
} from '@app/model/admin/thong-tin-ngoai-ngu-can-bo.model';
import {
  STAFF_JOB_HISTORY_KEY,
  StaffJobHistoryResponse,
} from '@app/model/admin/qua-trinh-cong-tac.model';
import {
  DiaChiHanhChinhService,
  DiaChiPhuongXaItem,
  DiaChiTinhThanhItem,
} from '@app/service/admin/dia-chi-hanh-chinh.service';
import { DialogThongTinDaoTaoComponent } from './thong-tin-dao-tao/dialog-thong-tin-dao-tao.component';
import { DialogThongTinNgoaiNguComponent } from './thong-tin-ngoai-ngu/dialog-thong-tin-ngoai-ngu.component';
import { DialogQuaTrinhCongTacComponent } from './qua-trinh-cong-tac/dialog-qua-trinh-cong-tac.component';
import { DialogPhanCongGiangDayComponent } from '../phan-cong-giang-day/dialog-phan-cong-giang-day/dialog-phan-cong-giang-day.component';
import { PhanCongGiangDayService } from '@app/service/admin/phan-cong-giang-day.service';
import { PhanCongGiangDayResponse } from '@app/model/admin/phan-cong-giang-day.model';
import {
  NguoiDungFilterRequest,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { NguoiDungService } from '@app/service/admin/nguoi-dung.service';
import { sha256 } from '@utils/utils';

type TabKey =
  | 'thong-tin-can-bo'
  | 'qua-trinh-cong-tac'
  | 'thong-tin-dao-tao'
  | 'thong-tin-luong'
  | 'thong-tin-ngoai-ngu'
  | 'phan-cong-giang-day';

const USER_ACCOUNT_STATUS_OPTIONS: IOptions[] = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Không hoạt động' },
];

@Component({
  selector: 'ho-so-can-bo',
  standalone: true,
  templateUrl: './ho-so-can-bo.component.html',
  styleUrls: ['./ho-so-can-bo.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconComponent,
    AppTableComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class HoSoCanBoComponent extends ComponentBaseAbstract {
  override readonly TYPE_FORM = TYPE_FORM;
  private readonly passwordRegex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_])[A-Za-z\d\W_]{8,}$/;

  readonly tabs: { key: TabKey; label: string }[] = [
    { key: 'thong-tin-can-bo', label: 'THÔNG TIN CÁN BỘ' },
    {
      key: 'qua-trinh-cong-tac',
      label: 'QUẢN LÝ QUÁ TRÌNH CÔNG TÁC CỦA CÁN BỘ',
    },
    { key: 'thong-tin-dao-tao', label: 'THÔNG TIN ĐÀO TẠO' },
    { key: 'thong-tin-luong', label: 'THÔNG TIN LƯƠNG' },
    { key: 'thong-tin-ngoai-ngu', label: 'THÔNG TIN NGOẠI NGỮ' },
    { key: 'phan-cong-giang-day', label: 'PHÂN CÔNG GIẢNG DẠY' },
  ];
  readonly genderOptions = CAN_BO_GENDER_OPTIONS;
  readonly statusOptions = CAN_BO_STATUS_OPTIONS;
  readonly jobHistoryTableConfig = { hasFilterPanel: false };
  readonly trainingTableConfig = { hasFilterPanel: false };
  readonly foreignLanguageTableConfig = { hasFilterPanel: false };
  readonly teachingAssignmentTableConfig = { hasFilterPanel: false };

  activeTab: TabKey = 'thong-tin-can-bo';
  staffId?: string;
  staff: CanBoDetailResponse = { ...CAN_BO_PROFILE_FALLBACK };
  hasExistingUserAccount = false;
  unitOptions: IOptions[] = [];
  gradeOptions: IOptions[] = [];
  profileItems: FormType[] = [];

  // Tables
  jobHistoryColumns: MtxGridColumn[] = [];
  jobHistoryDataSource: StaffJobHistoryResponse[] = [];
  jobHistoryTotal = 0;
  jobHistoryPageIndex = 0;
  jobHistoryPageSize = 10;
  trainingColumns: MtxGridColumn[] = [];
  trainingDataSource: StaffTrainingResponse[] = [];
  trainingTotal = 0;
  trainingPageIndex = 0;
  trainingPageSize = 10;
  foreignLanguageColumns: MtxGridColumn[] = [];
  foreignLanguageDataSource: StaffForeignLanguageResponse[] = [];
  foreignLanguageTotal = 0;
  foreignLanguagePageIndex = 0;
  foreignLanguagePageSize = 10;
  teachingAssignmentColumns: MtxGridColumn[] = [];
  teachingAssignmentDataSource: PhanCongGiangDayResponse[] = [];
  teachingAssignmentTotal = 0;
  teachingAssignmentPageIndex = 0;
  teachingAssignmentPageSize = 10;

  // Avatar
  selectedAvatarName = '';

  // Address dropdowns — 3 groups
  provinceOptions: IOptions[] = [];
  permanentProvinceItem!: FormType;
  permanentWardItem!: FormType;
  temporaryProvinceItem!: FormType;
  temporaryWardItem!: FormType;
  birthPlaceProvinceItem!: FormType;
  birthPlaceWardItem!: FormType;

  private provinceLookup = new Map<string, DiaChiTinhThanhItem>();
  private permanentWardLookup = new Map<string, DiaChiPhuongXaItem>();
  private temporaryWardLookup = new Map<string, DiaChiPhuongXaItem>();
  private birthPlaceWardLookup = new Map<string, DiaChiPhuongXaItem>();

  constructor(
    protected override injector: Injector,
    private readonly routeService: ActivatedRoute,
    private readonly routerService: Router,
    private readonly locationService: Location,
    private readonly canBoService: CanBoService,
    private readonly donViService: DonViService,
    private readonly khoiService: KhoiService,
    private readonly staffJobHistoryService: StaffJobHistoryService,
    private readonly staffTrainingService: StaffTrainingService,
    private readonly staffForeignLanguageService: StaffForeignLanguageService,
    private readonly diaChiHanhChinhService: DiaChiHanhChinhService,
    private readonly phanCongGiangDayService: PhanCongGiangDayService,
    private readonly nguoiDungService: NguoiDungService
  ) {
    super(injector);
  }

  get isDetailMode(): boolean {
    return this.pathType === this.TYPE_FORM.DETAIL;
  }

  get isUpdateMode(): boolean {
    return this.pathType === this.TYPE_FORM.UPDATE;
  }

  get visibleTabs(): { key: TabKey; label: string }[] {
    return this.tabs.filter((t) => t.key !== ('thong-tin-luong' as TabKey));
  }

  get avatarPreview(): string {
    return this.resolveAvatarUrl(
      this.form?.get('avatarUrl')?.value ?? this.staff.avatarUrl
    );
  }

  // ════════════════════════════════════════
  //  Lifecycle
  // ════════════════════════════════════════
  protected override componentInit(): void {
    this.syncPathType();
    this.staffId = this.routeService.snapshot.paramMap.get('id') ?? undefined;
    const tabParam = this.routeService.snapshot.queryParamMap.get('tab');
    if (this.isValidTab(tabParam)) this.activeTab = tabParam;
    this.initItems();
    this.initAddressItems();
    this.initForm();
    this.initJobHistoryColumns();
    this.initTrainingColumns();
    this.initForeignLanguageColumns();
    this.initTeachingAssignmentColumns();
    this.loadUnitOptions();
    this.loadGradeOptions();
    this.loadCreateUserRoleOptions();
    this.loadProvinces();
    this.bindGenerateCode();
    this.bindCreateAccount();
    this.bindRouteMode();
    this.bindAddressSelects();

    const routeState = history.state?.staff as CanBoDetailResponse | undefined;
    if (routeState) {
      this.staff = this.normalizeDetail(routeState);
      this.patchForm(this.staff);
      this.loadUserAccountInfo(this.staff.userId ?? undefined);
    }

    if (!this.staffId) return;

    this.canBoService.getById(this.staffId).subscribe({
      next: ({ data }) => {
        this.staff = this.normalizeDetail(data);
        this.patchForm(this.staff);
        this.loadUserAccountInfo(
          this.staff.userId ?? data?.userId ?? undefined
        );
        this.loadJobHistories({
          pageIndex: 0,
          pageSize: this.jobHistoryPageSize,
        });
        this.loadTrainingInfos({
          pageIndex: 0,
          pageSize: this.trainingPageSize,
        });
        this.loadForeignLanguages({
          pageIndex: 0,
          pageSize: this.foreignLanguagePageSize,
        });
        this.loadTeachingAssignments({
          pageIndex: 0,
          pageSize: this.teachingAssignmentPageSize,
        });
      },
      error: (error) => {
        if (!routeState) {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Tải dữ liệu thất bại',
            'Thất bại'
          );
        }
      },
    });
  }

  private loadUserAccountInfo(userId?: string | number): void {
    if (!userId) {
      this.loadUserAccountInfoByStaffId();
      return;
    }

    this.nguoiDungService.getById(userId).subscribe({
      next: ({ data }) => {
        this.patchAccountFormFromUser(data);
      },
      error: () => {
        this.loadUserAccountInfoByStaffId();
      },
    });
  }

  private loadUserAccountInfoByStaffId(): void {
    const normalizedStaffId = Number(this.staffId ?? 0);
    if (!normalizedStaffId) {
      this.patchAccountFormFromUser();
      return;
    }

    const payload = {
      pageNow: 1,
      pageSize: 1,
      filter: {
        staffId: normalizedStaffId,
      },
    } as unknown as NguoiDungFilterRequest;

    this.nguoiDungService.filter(payload).subscribe({
      next: ({ data }) => {
        const users = Array.isArray(data)
          ? data
          : Array.isArray(data?.items)
            ? data.items
            : [];
        this.patchAccountFormFromUser(users[0]);
      },
      error: () => {
        this.patchAccountFormFromUser();
      },
    });
  }

  private patchAccountFormFromUser(user?: NguoiDungResponse): void {
    const createAccountControl = this.form.get('createAccount');
    const usernameControl = this.form.get('username');
    const passwordControl = this.form.get('password');
    const roleIdControl = this.form.get('roleId');
    const accountStatusControl = this.form.get('accountStatus');
    const sendActivationEmailControl = this.form.get('sendActivationEmail');

    if (!user) {
      this.hasExistingUserAccount = false;
      this.staff.userId = null;
      createAccountControl?.setValue(false, { emitEvent: true });
      usernameControl?.setValue('', { emitEvent: false });
      passwordControl?.setValue('', { emitEvent: false });
      roleIdControl?.setValue(null, { emitEvent: false });
      accountStatusControl?.setValue(null, { emitEvent: false });
      sendActivationEmailControl?.setValue(false, { emitEvent: false });
      return;
    }

    this.hasExistingUserAccount = true;
    this.staff.userId = user.id ?? this.staff.userId ?? null;
    createAccountControl?.setValue(true, { emitEvent: true });
    usernameControl?.setValue(user.username ?? '', { emitEvent: false });
    passwordControl?.setValue('', { emitEvent: false });
    roleIdControl?.setValue(user.roleId ?? null, { emitEvent: false });
    accountStatusControl?.setValue(user.status ?? 1, { emitEvent: false });
    sendActivationEmailControl?.setValue(false, { emitEvent: false });
  }

  // ════════════════════════════════════════
  //  Tab / Navigation
  // ════════════════════════════════════════
  selectTab(tabKey: TabKey): void {
    this.activeTab = tabKey;
    this.routerService.navigate([], {
      relativeTo: this.routeService,
      queryParams: { tab: tabKey },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  goBack(): void {
    this.locationService.back();
  }

  openEdit(): void {
    if (!this.staffId) return;
    this.pathType = this.TYPE_FORM.UPDATE;
    this.routerService.navigate(
      [
        '/',
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
        PATH.CAP_NHAT,
        this.staffId,
      ],
      { state: { staff: this.staff } }
    );
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.buildPayload();

    if (this.pathType === this.TYPE_FORM.CREATE) {
      console.log('Payload create staff:', payload);
      this.canBoService.create(payload).subscribe({
        next: async ({ data }) => {
          const createdUser = await this.syncUserAccountForStaff(
            data?.id,
            data?.userId
          );
          if (!createdUser) {
            this.toastr.warning(
              'Đã tạo cán bộ nhưng tạo tài khoản đăng nhập thất bại',
              'Cảnh báo'
            );
            return;
          }

          this.toastr.success('Thêm cán bộ thành công', 'Thành công');
          this.routerService.navigate([
            '/',
            NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
            ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
          ]);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Lưu thất bại',
            'Thất bại'
          );
        },
      });
      return;
    }

    if (!this.staffId || !this.isUpdateMode) return;
    console.log('Payload update staff:', payload);
    this.canBoService.update(this.staffId, payload).subscribe({
      next: async ({ data }) => {
        const syncedUser = await this.syncUserAccountForStaff(
          data?.id ?? this.staffId,
          data?.userId ?? this.staff.userId
        );
        if (!syncedUser) {
          this.toastr.warning(
            'Đã cập nhật cán bộ nhưng đồng bộ tài khoản đăng nhập thất bại',
            'Cảnh báo'
          );
          return;
        }

        this.toastr.success('Cập nhật thành công', 'Thành công');
        this.routerService.navigate([
          '/',
          NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
          ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
        ]);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu thất bại',
          'Thất bại'
        );
      },
    });
  }

  private async syncUserAccountForStaff(
    staffId?: string | number | null,
    existingUserId?: string | number | null
  ): Promise<boolean> {
    const v = this.form.getRawValue();
    if (!v.createAccount) {
      return true;
    }

    const normalizedStaffId = Number(staffId ?? 0);
    if (!normalizedStaffId) {
      this.toastr.error(
        'Không lấy được staffId để liên kết tài khoản người dùng',
        'Thất bại'
      );
      return false;
    }

    try {
      const userPayload: NguoiDungFormRequest & {
        email?: string;
        unitId?: number;
        fullName?: string;
      } = {
        username: v.username ?? '',
        roleId: `${v.roleId ?? ''}`,
        status: this.mapUserStatus(v.accountStatus),
        staffId: normalizedStaffId,
        email: v.email ?? '',
        unitId: Number(v.unitId ?? 0),
        fullName: v.fullName ?? '',
      };

      if (existingUserId) {
        const passwordValue = `${v.password ?? ''}`.trim();
        const updatePayload: NguoiDungFormRequest & {
          email?: string;
          unitId?: number;
          fullName?: string;
        } = {
          id: existingUserId,
          ...userPayload,
        };

        if (passwordValue) {
          updatePayload.password = await sha256(passwordValue);
        }

        await firstValueFrom(this.nguoiDungService.update(updatePayload));
      } else {
        const createPayload: NguoiDungFormRequest & {
          email?: string;
          unitId?: number;
          fullName?: string;
        } = {
          ...userPayload,
        };

        const passwordValue = `${v.password ?? ''}`.trim();
        if (passwordValue) {
          createPayload.password = await sha256(passwordValue);
        }

        await firstValueFrom(this.nguoiDungService.create(createPayload));
      }

      return true;
    } catch (error: unknown) {
      const e = error as {
        error?: {
          userMessage?: string;
          message?: string;
        };
      };
      this.toastr.error(
        e?.error?.userMessage ??
          e?.error?.message ??
          'Không tạo được tài khoản đăng nhập',
        'Thất bại'
      );
      return false;
    }
  }

  private mapUserStatus(value: unknown): number {
    if (typeof value === 'number') {
      return value;
    }

    const normalized = `${value ?? ''}`.trim().toUpperCase();
    if (
      normalized === 'ACTIVE' ||
      normalized === '1' ||
      normalized === 'TRUE'
    ) {
      return 1;
    }

    return 0;
  }

  // ════════════════════════════════════════
  //  Display helpers
  // ════════════════════════════════════════
  getValue(value: unknown): string {
    if (value === null || value === undefined || value === '') return '—';
    return `${value}`;
  }

  getGenderLabel(value?: string | null): string {
    const n = `${value ?? ''}`.trim().toUpperCase();
    if (!n) return '—';
    if (n === 'MALE' || n === 'NAM' || n === '0') return 'Nam';
    if (n === 'FEMALE' || n === 'NU' || n === '1') return 'Nữ';
    return `${value ?? ''}`;
  }

  getStatusLabel(value?: string | null): string {
    const n = `${value ?? ''}`.trim().toUpperCase();
    if (!n) return '—';
    return (
      this.statusOptions.find((o) => `${o.value}`.toUpperCase() === n)?.label ??
      `${value ?? ''}`
    );
  }

  // ════════════════════════════════════════
  //  Avatar
  // ════════════════════════════════════════
  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.selectedAvatarName = file.name;
    const reader = new FileReader();
    reader.onload = () => {
      this.form.get('avatarUrl')?.setValue(`${reader.result ?? ''}`);
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  onAvatarCardKeydown(event: KeyboardEvent, input: HTMLInputElement): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      input.click();
    }
  }

  // ════════════════════════════════════════
  //  Table data loaders
  // ════════════════════════════════════════
  loadJobHistories(query?: TableQueryEvent): void {
    if (!this.staffId) return;
    this.jobHistoryPageIndex = query?.pageIndex ?? this.jobHistoryPageIndex;
    this.jobHistoryPageSize = query?.pageSize ?? this.jobHistoryPageSize;
    this.staffJobHistoryService
      .filter({
        pageNow: this.jobHistoryPageIndex + 1,
        pageSize: this.jobHistoryPageSize,
        filter: { staffId: Number(this.staffId) },
      })
      .subscribe({
        next: ({ data }) => {
          const items = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
              ? data.items
              : [];
          this.jobHistoryDataSource = items.map((i) => ({
            ...i,
            unitName: this.getUnitLabel(i.unitId),
          }));
          this.jobHistoryTotal = Array.isArray(data)
            ? items.length
            : (data?.recordTotal ?? items.length);
        },
        error: () => {
          this.jobHistoryDataSource = [];
          this.jobHistoryTotal = 0;
        },
      });
  }

  loadTrainingInfos(query?: TableQueryEvent): void {
    if (!this.staffId) return;
    this.trainingPageIndex = query?.pageIndex ?? this.trainingPageIndex;
    this.trainingPageSize = query?.pageSize ?? this.trainingPageSize;
    this.staffTrainingService
      .filter({
        pageNow: this.trainingPageIndex + 1,
        pageSize: this.trainingPageSize,
        filter: { staffId: Number(this.staffId) },
      })
      .subscribe({
        next: ({ data }) => {
          const items = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
              ? data.items
              : [];
          this.trainingDataSource = items;
          this.trainingTotal = Array.isArray(data)
            ? items.length
            : (data?.recordTotal ?? items.length);
        },
        error: () => {
          this.trainingDataSource = [];
          this.trainingTotal = 0;
        },
      });
  }

  loadForeignLanguages(query?: TableQueryEvent): void {
    if (!this.staffId) return;
    this.foreignLanguagePageIndex =
      query?.pageIndex ?? this.foreignLanguagePageIndex;
    this.foreignLanguagePageSize =
      query?.pageSize ?? this.foreignLanguagePageSize;
    this.staffForeignLanguageService
      .filter({
        pageNow: this.foreignLanguagePageIndex + 1,
        pageSize: this.foreignLanguagePageSize,
        filter: { staffId: Number(this.staffId) },
      })
      .subscribe({
        next: ({ data }) => {
          const items = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
              ? data.items
              : [];
          this.foreignLanguageDataSource = items;
          this.foreignLanguageTotal = Array.isArray(data)
            ? items.length
            : (data?.recordTotal ?? items.length);
        },
        error: () => {
          this.foreignLanguageDataSource = [];
          this.foreignLanguageTotal = 0;
        },
      });
  }

  loadTeachingAssignments(query?: TableQueryEvent): void {
    if (!this.staffId) return;
    this.teachingAssignmentPageIndex =
      query?.pageIndex ?? this.teachingAssignmentPageIndex;
    this.teachingAssignmentPageSize =
      query?.pageSize ?? this.teachingAssignmentPageSize;
    this.phanCongGiangDayService
      .filter({
        pageNow: this.teachingAssignmentPageIndex + 1,
        pageSize: this.teachingAssignmentPageSize,
        filter: { staffId: Number(this.staffId) },
      })
      .subscribe({
        next: ({ data }) => {
          const items = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
              ? data.items
              : [];
          this.teachingAssignmentDataSource = items;
          this.teachingAssignmentTotal = Array.isArray(data)
            ? items.length
            : (data?.recordTotal ?? items.length);
        },
        error: () => {
          this.teachingAssignmentDataSource = [];
          this.teachingAssignmentTotal = 0;
        },
      });
  }

  // ════════════════════════════════════════
  //  Dialog openers
  // ════════════════════════════════════════
  openJobHistoryDialog(
    type: TYPE_FORM_KEY,
    rowData?: StaffJobHistoryResponse
  ): void {
    if (!this.staffId) return;
    this.dialog.componentDialog(
      DialogQuaTrinhCongTacComponent,
      {
        width: '720px',
        data: {
          type,
          staffId: Number(this.staffId),
          id: rowData?.id,
          data: rowData,
          unitOptions: this.unitOptions,
        },
      },
      (result?: boolean) => {
        if (result)
          this.loadJobHistories({
            pageIndex: this.jobHistoryPageIndex,
            pageSize: this.jobHistoryPageSize,
          });
      }
    );
  }

  deleteJobHistory(rowData: StaffJobHistoryResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa quá trình công tác ${rowData.decisionNo ?? ''} không?`,
      },
      (ok?: boolean) => {
        if (!ok) return;
        this.staffJobHistoryService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadJobHistories({
              pageIndex: this.jobHistoryPageIndex,
              pageSize: this.jobHistoryPageSize,
            });
          },
          error: (e) => {
            this.toastr.error(
              e?.error?.userMessage ?? e?.error?.message ?? 'Xóa thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  openTrainingDialog(
    type: TYPE_FORM_KEY,
    rowData?: StaffTrainingResponse
  ): void {
    if (!this.staffId) return;
    this.dialog.componentDialog(
      DialogThongTinDaoTaoComponent,
      {
        width: '720px',
        data: {
          type,
          staffId: Number(this.staffId),
          id: rowData?.id,
          data: rowData,
        },
      },
      (result?: boolean) => {
        if (result)
          this.loadTrainingInfos({
            pageIndex: this.trainingPageIndex,
            pageSize: this.trainingPageSize,
          });
      }
    );
  }

  deleteTraining(rowData: StaffTrainingResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa thông tin đào tạo ${rowData.schoolName ?? ''} không?`,
      },
      (ok?: boolean) => {
        if (!ok) return;
        this.staffTrainingService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadTrainingInfos({
              pageIndex: this.trainingPageIndex,
              pageSize: this.trainingPageSize,
            });
          },
          error: (e) => {
            this.toastr.error(
              e?.error?.userMessage ?? e?.error?.message ?? 'Xóa thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  openForeignLanguageDialog(
    type: TYPE_FORM_KEY,
    rowData?: StaffForeignLanguageResponse
  ): void {
    if (!this.staffId) return;
    this.dialog.componentDialog(
      DialogThongTinNgoaiNguComponent,
      {
        width: '720px',
        data: {
          type,
          staffId: Number(this.staffId),
          id: rowData?.id,
          data: rowData,
        },
      },
      (result?: boolean) => {
        if (result)
          this.loadForeignLanguages({
            pageIndex: this.foreignLanguagePageIndex,
            pageSize: this.foreignLanguagePageSize,
          });
      }
    );
  }

  deleteForeignLanguage(rowData: StaffForeignLanguageResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa thông tin ngoại ngữ ${rowData.languageName ?? ''} không?`,
      },
      (ok?: boolean) => {
        if (!ok) return;
        this.staffForeignLanguageService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadForeignLanguages({
              pageIndex: this.foreignLanguagePageIndex,
              pageSize: this.foreignLanguagePageSize,
            });
          },
          error: (e) => {
            this.toastr.error(
              e?.error?.userMessage ?? e?.error?.message ?? 'Xóa thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  openTeachingAssignmentDialog(
    type: TYPE_FORM_KEY,
    rowData?: PhanCongGiangDayResponse
  ): void {
    if (!this.staffId) return;
    this.dialog.componentDialog(
      DialogPhanCongGiangDayComponent,
      {
        width: '1080px',
        data: {
          type,
          staffId: Number(this.staffId),
          id: rowData?.id,
          data: rowData,
        },
      },
      (result?: boolean) => {
        if (result) {
          this.goBack();
        }
      }
    );
  }

  deleteTeachingAssignment(rowData: PhanCongGiangDayResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa phân công giảng dạy này không?`,
      },
      (ok?: boolean) => {
        if (!ok) return;
        this.phanCongGiangDayService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadTeachingAssignments({
              pageIndex: this.teachingAssignmentPageIndex,
              pageSize: this.teachingAssignmentPageSize,
            });
          },
          error: (e) => {
            this.toastr.error(
              e?.error?.userMessage ?? e?.error?.message ?? 'Xóa thất bại',
              'Thất bại'
            );
          },
        });
      }
    );
  }

  // ════════════════════════════════════════
  //  Form item definitions
  // ════════════════════════════════════════
  private initItems(): void {
    this.profileItems = [
      SELECT_CONTROL({
        controlName: 'unitId',
        label: 'Đơn vị',
        placeholder: 'Chọn đơn vị',
        required: true,
        clearable: true,
        listOption: [],
      }),
      // Hidden
      TEXT_CONTROL({
        controlName: 'avatarUrl',
        label: '',
        placeholder: '',
        required: false,
        hidden: true,
      }),
      // Basic info
      TEXT_CONTROL({
        controlName: 'staffCode',
        label: 'Mã cán bộ',
        placeholder: 'Mã cán bộ',
        required: false,
        disabled: true,
      }),
      TEXT_CONTROL({
        controlName: 'fullName',
        label: 'Họ và tên',
        placeholder: 'Họ và tên',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'aliasName',
        label: 'Tên gọi khác',
        placeholder: 'Tên gọi khác',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'identityCode',
        label: 'Mã định danh',
        placeholder: 'Mã định danh',
        required: false,
      }),
      SELECT_CONTROL({
        controlName: 'gender',
        label: 'Giới tính',
        placeholder: 'Giới tính',
        required: false,
        clearable: true,
        listOption: this.genderOptions,
      }),
      DATE_CONTROL({
        controlName: 'dateOfBirth',
        label: 'Ngày sinh',
        placeholder: 'Ngày sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'hometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      SELECT_CONTROL({
        controlName: 'ethnicityId',
        label: 'Dân tộc',
        placeholder: 'Dân tộc',
        required: false,
        clearable: true,
        listOption: DAN_TOC_OPTIONS,
      }),
      TEXT_CONTROL({
        controlName: 'religionId',
        label: 'Tôn giáo',
        placeholder: 'Tôn giáo',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'nationalityId',
        label: 'Quốc tịch',
        placeholder: 'Quốc tịch',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'phone',
        label: 'Điện thoại',
        placeholder: 'Điện thoại',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'email',
        label: 'Email',
        placeholder: 'Email',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'cccdNo',
        label: 'CCCD',
        placeholder: 'CCCD',
        required: false,
      }),
      DATE_CONTROL({
        controlName: 'cccdIssueDate',
        label: 'Ngày cấp CCCD',
        placeholder: 'Ngày cấp CCCD',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'cccdIssuePlace',
        label: 'Nơi cấp CCCD',
        placeholder: 'Nơi cấp CCCD',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'socialInsuranceNo',
        label: 'Số BHXH',
        placeholder: 'Số BHXH',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'healthStatus',
        label: 'Tình trạng sức khỏe',
        placeholder: 'Tình trạng sức khỏe',
        required: false,
      }),
      SELECT_CONTROL({
        controlName: 'gradeId',
        label: 'Khối',
        placeholder: 'Khối',
        required: false,
        clearable: true,
        listOption: [],
      }),
      SELECT_CONTROL({
        controlName: 'status',
        label: 'Trạng thái',
        placeholder: 'Trạng thái',
        required: false,
        clearable: true,
        listOption: this.statusOptions,
      }),
      TEXTAREA_CONTROL({
        controlName: 'note',
        label: 'Ghi chú',
        placeholder: 'Ghi chú',
        required: false,
        rows: 4,
      }),
      // Addresses (detail text)
      TEXT_CONTROL({
        controlName: 'permanentAddress',
        label: 'Địa chỉ chi tiết',
        placeholder: 'Địa chỉ chi tiết',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'temporaryAddress',
        label: 'Địa chỉ chi tiết',
        placeholder: 'Địa chỉ chi tiết',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'birthPlace',
        label: 'Địa chỉ chi tiết',
        placeholder: 'Địa chỉ chi tiết',
        required: false,
      }),
      // ── Family: Father ──
      TEXT_CONTROL({
        controlName: 'fatherName',
        label: 'Họ và tên',
        placeholder: 'Họ và tên cha',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'fatherBirthYear',
        label: 'Năm sinh',
        placeholder: 'Năm sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'fatherBirthPlace',
        label: 'Nơi sinh',
        placeholder: 'Nơi sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'fatherHometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'fatherOccupation',
        label: 'Nghề nghiệp',
        placeholder: 'Nghề nghiệp',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'fatherPhone',
        label: 'Điện thoại',
        placeholder: 'Điện thoại',
        required: false,
      }),
      // ── Family: Mother ──
      TEXT_CONTROL({
        controlName: 'motherName',
        label: 'Họ và tên',
        placeholder: 'Họ và tên mẹ',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherBirthYear',
        label: 'Năm sinh',
        placeholder: 'Năm sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherBirthPlace',
        label: 'Nơi sinh',
        placeholder: 'Nơi sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherHometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherOccupation',
        label: 'Nghề nghiệp',
        placeholder: 'Nghề nghiệp',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherPhone',
        label: 'Điện thoại',
        placeholder: 'Điện thoại',
        required: false,
      }),
      // ── Family: Spouse ──
      TEXT_CONTROL({
        controlName: 'spouseName',
        label: 'Họ và tên',
        placeholder: 'Họ và tên vợ/chồng',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseBirthYear',
        label: 'Năm sinh',
        placeholder: 'Năm sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseBirthPlace',
        label: 'Nơi sinh',
        placeholder: 'Nơi sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseHometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseOccupation',
        label: 'Nghề nghiệp',
        placeholder: 'Nghề nghiệp',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spousePhone',
        label: 'Điện thoại',
        placeholder: 'Điện thoại',
        required: false,
      }),
      // ── Family: Spouse's father ──
      TEXT_CONTROL({
        controlName: 'spouseFatherName',
        label: 'Họ và tên',
        placeholder: 'Họ và tên bố chồng/vợ',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseFatherBirthYear',
        label: 'Năm sinh',
        placeholder: 'Năm sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseFatherBirthPlace',
        label: 'Nơi sinh',
        placeholder: 'Nơi sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseFatherHometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseFatherOccupation',
        label: 'Nghề nghiệp',
        placeholder: 'Nghề nghiệp',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseFatherPhone',
        label: 'Điện thoại',
        placeholder: 'Điện thoại',
        required: false,
      }),
      // ── Family: Mother-in-law ──
      TEXT_CONTROL({
        controlName: 'motherInLawName',
        label: 'Họ và tên',
        placeholder: 'Họ và tên mẹ chồng/vợ',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherInLawBirthYear',
        label: 'Năm sinh',
        placeholder: 'Năm sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherInLawBirthPlace',
        label: 'Nơi sinh',
        placeholder: 'Nơi sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherInLawHometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherInLawOccupation',
        label: 'Nghề nghiệp',
        placeholder: 'Nghề nghiệp',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherInLawPhone',
        label: 'Điện thoại',
        placeholder: 'Điện thoại',
        required: false,
      }),
      // ── Children ──
      TEXTAREA_CONTROL({
        controlName: 'childrenInfo',
        label: 'Thông tin con',
        placeholder: 'Thông tin con',
        required: false,
        rows: 4,
      }),
      // Account info
      CHECKBOX_CONTROL({
        controlName: 'createAccount',
        label: 'Tạo tài khoản đăng nhập',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'username',
        label: 'Tên đăng nhập',
        placeholder: 'Tên đăng nhập',
        required: true,
        disabled: false,
      }),
      TEXT_CONTROL({
        controlName: 'password',
        label: 'Mật khẩu',
        placeholder: 'Mật khẩu',
        required: false,
        hidden: true,
        type: 'password',
        regex: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_])[A-Za-z\d\W_]{8,}$/,
        hint: 'Tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, ký tự số và ký tự đặc biệt',
      }),
      SELECT_CONTROL({
        controlName: 'roleId',
        label: 'Vai trò',
        placeholder: 'Chọn vai trò',
        required: true,
        clearable: true,
        listOption: [],
      }),
      SELECT_CONTROL({
        controlName: 'accountStatus',
        label: 'Trạng thái',
        placeholder: 'Chọn trạng thái',
        required: true,
        clearable: true,
        listOption: [],
      }),
      CHECKBOX_CONTROL({
        controlName: 'sendActivationEmail',
        label: 'Gửi email kích hoạt',
        required: false,
      }),
    ];

    // Set account options
    const roleIdItem = this.findFormControl(this.profileItems, 'roleId');
    const accountStatusItem = this.findFormControl(
      this.profileItems,
      'accountStatus'
    );
    if (roleIdItem) {
      roleIdItem.options = [];
    }
    if (accountStatusItem) {
      accountStatusItem.options = USER_ACCOUNT_STATUS_OPTIONS;
    }
  }

  private initAddressItems(): void {
    this.permanentProvinceItem = SELECT_CONTROL({
      controlName: 'permanentProvinceName',
      label: 'Tỉnh/Thành phố',
      placeholder: 'Chọn tỉnh/thành phố',
      required: false,
      clearable: true,
      listOption: [],
    });
    this.permanentWardItem = SELECT_CONTROL({
      controlName: 'permanentWardName',
      label: 'Quận/Huyện/Xã',
      placeholder: 'Chọn quận/huyện/xã',
      required: false,
      clearable: true,
      listOption: [],
      disabled: true,
    });
    this.temporaryProvinceItem = SELECT_CONTROL({
      controlName: 'temporaryProvinceName',
      label: 'Tỉnh/Thành phố',
      placeholder: 'Chọn tỉnh/thành phố',
      required: false,
      clearable: true,
      listOption: [],
    });
    this.temporaryWardItem = SELECT_CONTROL({
      controlName: 'temporaryWardName',
      label: 'Quận/Huyện/Xã',
      placeholder: 'Chọn quận/huyện/xã',
      required: false,
      clearable: true,
      listOption: [],
      disabled: true,
    });
    this.birthPlaceProvinceItem = SELECT_CONTROL({
      controlName: 'birthPlaceProvinceName',
      label: 'Tỉnh/Thành phố',
      placeholder: 'Chọn tỉnh/thành phố',
      required: false,
      clearable: true,
      listOption: [],
    });
    this.birthPlaceWardItem = SELECT_CONTROL({
      controlName: 'birthPlaceWardName',
      label: 'Quận/Huyện/Xã',
      placeholder: 'Chọn quận/huyện/xã',
      required: false,
      clearable: true,
      listOption: [],
      disabled: true,
    });
  }

  private initForm(): void {
    const allItems = [
      ...this.profileItems,
      this.permanentProvinceItem,
      this.permanentWardItem,
      this.temporaryProvinceItem,
      this.temporaryWardItem,
      this.birthPlaceProvinceItem,
      this.birthPlaceWardItem,
    ];
    this.form = this.itemControl.toFormGroup(allItems);
  }

  // ════════════════════════════════════════
  //  Table columns
  // ════════════════════════════════════════
  private initJobHistoryColumns(): void {
    this.jobHistoryColumns = [
      { header: 'STT', field: COMMON_TABLE_KEY.STT, class: 'text-center' },
      { header: 'Từ ngày', field: STAFF_JOB_HISTORY_KEY.FROM_DATE },
      { header: 'Đến ngày', field: STAFF_JOB_HISTORY_KEY.TO_DATE },
      { header: 'Đơn vị', field: 'unitName' },
      {
        header: 'Phòng ban / Tổ / Bộ phận',
        field: STAFF_JOB_HISTORY_KEY.DEPARTMENT_ID,
      },
      { header: 'Chức danh', field: STAFF_JOB_HISTORY_KEY.WORKING_POSITION_ID },
      { header: 'Chức vụ', field: STAFF_JOB_HISTORY_KEY.TITLE_ID },
      {
        header: 'Loại tuyển dụng',
        field: STAFF_JOB_HISTORY_KEY.EMPLOYMENT_TYPE_ID,
      },
      { header: 'Số quyết định', field: STAFF_JOB_HISTORY_KEY.DECISION_NO },
      { header: 'Ghi chú', field: STAFF_JOB_HISTORY_KEY.NOTE },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (r: StaffJobHistoryResponse) =>
              this.openJobHistoryDialog(this.TYPE_FORM.UPDATE, r),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (r: StaffJobHistoryResponse) => this.deleteJobHistory(r),
          },
        ],
      },
    ];
  }

  private initTrainingColumns(): void {
    this.trainingColumns = [
      { header: 'STT', field: COMMON_TABLE_KEY.STT, class: 'text-center' },
      { header: 'Trường đào tạo', field: STAFF_TRAINING_KEY.SCHOOL_NAME },
      { header: 'Chuyên ngành', field: STAFF_TRAINING_KEY.MAJOR },
      { header: 'Hình thức đào tạo', field: STAFF_TRAINING_KEY.TRAINING_FORM },
      { header: 'Chứng chỉ', field: STAFF_TRAINING_KEY.CERTIFICATE },
      { header: 'Từ ngày', field: STAFF_TRAINING_KEY.FROM_DATE },
      { header: 'Đến ngày', field: STAFF_TRAINING_KEY.TO_DATE },
      { header: 'Ghi chú', field: STAFF_TRAINING_KEY.NOTE },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (r: StaffTrainingResponse) =>
              this.openTrainingDialog(this.TYPE_FORM.UPDATE, r),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (r: StaffTrainingResponse) => this.deleteTraining(r),
          },
        ],
      },
    ];
  }

  private initForeignLanguageColumns(): void {
    this.foreignLanguageColumns = [
      { header: 'STT', field: COMMON_TABLE_KEY.STT, class: 'text-center' },
      { header: 'Ngoại ngữ', field: STAFF_FOREIGN_LANGUAGE_KEY.LANGUAGE_NAME },
      { header: 'Trình độ', field: STAFF_FOREIGN_LANGUAGE_KEY.LANGUAGE_LEVEL },
      { header: 'Ngày cấp', field: STAFF_FOREIGN_LANGUAGE_KEY.ISSUE_DATE },
      { header: 'Điểm số', field: STAFF_FOREIGN_LANGUAGE_KEY.SCORE },
      { header: 'Ghi chú', field: STAFF_FOREIGN_LANGUAGE_KEY.NOTE },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (r: StaffForeignLanguageResponse) =>
              this.openForeignLanguageDialog(this.TYPE_FORM.UPDATE, r),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (r: StaffForeignLanguageResponse) =>
              this.deleteForeignLanguage(r),
          },
        ],
      },
    ];
  }

  private initTeachingAssignmentColumns(): void {
    this.teachingAssignmentColumns = [
      { header: 'STT', field: COMMON_TABLE_KEY.STT, class: 'text-center' },
      { header: 'Năm học', field: 'schoolYearName' },
      { header: 'Lớp', field: 'className' },
      { header: 'Môn học', field: 'subjectName' },
      { header: 'Tổ/Phòng ban', field: 'departmentName' },
      { header: 'Số tiết', field: 'teachingLoad' },
      { header: 'Chủ nhiệm', field: 'isHomeroom' },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            class: 'action-edit',
            tooltip: 'Chỉnh sửa',
            click: (r: PhanCongGiangDayResponse) =>
              this.openTeachingAssignmentDialog(this.TYPE_FORM.UPDATE, r),
          },
          {
            type: 'icon',
            icon: 'delete',
            class: 'action-delete',
            tooltip: 'Xóa',
            click: (r: PhanCongGiangDayResponse) =>
              this.deleteTeachingAssignment(r),
          },
        ],
      },
    ];
  }

  // ════════════════════════════════════════
  //  Bindings
  // ════════════════════════════════════════
  private bindCreateAccount(): void {
    const createAccountControl = this.form.get('createAccount');
    const usernameItem = this.findFormControl(this.profileItems, 'username');
    const passwordItem = this.findFormControl(this.profileItems, 'password');
    const roleIdItem = this.findFormControl(this.profileItems, 'roleId');
    const accountStatusItem = this.findFormControl(
      this.profileItems,
      'accountStatus'
    );
    const sendActivationEmailItem = this.findFormControl(
      this.profileItems,
      'sendActivationEmail'
    );
    const usernameControl = this.form.get('username');
    const passwordControl = this.form.get('password');
    const roleIdControl = this.form.get('roleId');
    const accountStatusControl = this.form.get('accountStatus');

    const applyCreateAccountState = (isChecked: boolean): void => {
      if (isChecked) {
        const hidePasswordInUpdate =
          this.isUpdateMode && this.hasExistingUserAccount;

        // Show all fields and make them required
        usernameItem.hidden = false;
        passwordItem.hidden = hidePasswordInUpdate;
        roleIdItem.hidden = false;
        accountStatusItem.hidden = false;
        sendActivationEmailItem.hidden = false;

        // Set required: true
        usernameItem.required = true;
        passwordItem.required = !hidePasswordInUpdate;
        roleIdItem.required = true;
        accountStatusItem.required = true;

        // Enable controls and add validators
        usernameControl?.setValidators([Validators.required]);
        usernameControl?.updateValueAndValidity({ emitEvent: false });
        usernameControl?.enable({ emitEvent: false });

        if (hidePasswordInUpdate) {
          passwordControl?.setValue('', { emitEvent: false });
          passwordControl?.clearValidators();
          passwordControl?.updateValueAndValidity({ emitEvent: false });
          passwordControl?.disable({ emitEvent: false });
        } else {
          passwordControl?.setValidators([
            Validators.required,
            Validators.pattern(this.passwordRegex),
          ]);
          passwordControl?.updateValueAndValidity({ emitEvent: false });
          passwordControl?.enable({ emitEvent: false });
        }

        roleIdControl?.setValidators([Validators.required]);
        roleIdControl?.updateValueAndValidity({ emitEvent: false });
        roleIdControl?.enable({ emitEvent: false });

        accountStatusControl?.setValidators([Validators.required]);
        accountStatusControl?.updateValueAndValidity({ emitEvent: false });
        accountStatusControl?.enable({ emitEvent: false });
        if (
          accountStatusControl?.value === null ||
          accountStatusControl?.value === undefined ||
          accountStatusControl?.value === ''
        ) {
          accountStatusControl?.setValue(1, { emitEvent: false });
        }
        return;
      }

      // Hide all fields and make them optional
      usernameItem.hidden = true;
      passwordItem.hidden = true;
      roleIdItem.hidden = true;
      accountStatusItem.hidden = true;
      sendActivationEmailItem.hidden = true;

      // Set required: false
      usernameItem.required = false;
      passwordItem.required = false;
      roleIdItem.required = false;
      accountStatusItem.required = false;

      // Clear values and remove validators
      usernameControl?.setValue('', { emitEvent: false });
      usernameControl?.clearValidators();
      usernameControl?.updateValueAndValidity({ emitEvent: false });

      passwordControl?.setValue('', { emitEvent: false });
      passwordControl?.clearValidators();
      passwordControl?.updateValueAndValidity({ emitEvent: false });

      roleIdControl?.setValue(null, { emitEvent: false });
      roleIdControl?.clearValidators();
      roleIdControl?.updateValueAndValidity({ emitEvent: false });

      accountStatusControl?.setValue(null, { emitEvent: false });
      accountStatusControl?.clearValidators();
      accountStatusControl?.updateValueAndValidity({ emitEvent: false });

      const sendActivationEmailControl = this.form.get('sendActivationEmail');
      sendActivationEmailControl?.setValue(false, { emitEvent: false });

      usernameControl?.disable({ emitEvent: false });
      passwordControl?.disable({ emitEvent: false });
      roleIdControl?.disable({ emitEvent: false });
      accountStatusControl?.disable({ emitEvent: false });
    };

    if (createAccountControl) {
      createAccountControl.setValue(!!createAccountControl.value, {
        emitEvent: false,
      });
      applyCreateAccountState(!!createAccountControl.value);

      createAccountControl.valueChanges
        .pipe(takeUntil(this.ngUnsubscribe))
        .subscribe((isChecked) => {
          applyCreateAccountState(!!isChecked);
        });
    }
  }

  private bindGenerateCode(): void {
    const staffCodeControl = this.form.get('staffCode');
    const unitIdControl = this.form.get('unitId');
    staffCodeControl?.disable({ emitEvent: false });
    if (this.pathType === this.TYPE_FORM.CREATE) {
      unitIdControl?.valueChanges
        .pipe(takeUntil(this.ngUnsubscribe))
        .subscribe((unitId) => {
          if (!unitId) {
            staffCodeControl?.setValue('', { emitEvent: false });
            return;
          }
          this.canBoService.generateCode(unitId).subscribe(({ data }) => {
            staffCodeControl?.setValue(data, { emitEvent: false });
          });
        });
    }
  }

  private bindRouteMode(): void {
    this.routerService.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.ngUnsubscribe)
      )
      .subscribe(() => this.syncPathType());
  }

  private bindAddressSelects(): void {
    // Permanent province → wards
    this.form
      .get('permanentProvinceName')
      ?.valueChanges.pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((code) => {
        this.loadWardOptions(
          `${code ?? ''}`,
          this.permanentWardItem,
          'permanentWardName',
          this.permanentWardLookup
        );
      });
    // Temporary province → wards
    this.form
      .get('temporaryProvinceName')
      ?.valueChanges.pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((code) => {
        this.loadWardOptions(
          `${code ?? ''}`,
          this.temporaryWardItem,
          'temporaryWardName',
          this.temporaryWardLookup
        );
      });
    // BirthPlace province → wards
    this.form
      .get('birthPlaceProvinceName')
      ?.valueChanges.pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((code) => {
        this.loadWardOptions(
          `${code ?? ''}`,
          this.birthPlaceWardItem,
          'birthPlaceWardName',
          this.birthPlaceWardLookup
        );
      });
  }

  private syncPathType(): void {
    this.getTypeByPath();
  }

  private isValidTab(value: string | null): value is TabKey {
    return this.tabs.some((tab) => tab.key === value);
  }

  // ════════════════════════════════════════
  //  Data loaders
  // ════════════════════════════════════════
  private loadUnitOptions(): void {
    this.donViService.getOptions().subscribe({
      next: ({ data }) => {
        this.unitOptions = (data ?? []).map((i) => ({
          value: i.id,
          label: i.name,
        }));

        this.findFormControl(this.profileItems, 'unitId').options =
          this.unitOptions;
      },
    });
  }

  private loadGradeOptions(): void {
    this.khoiService.getOptions().subscribe({
      next: ({ data }) => {
        this.gradeOptions = (data ?? []).map((i) => ({
          value: i.id,
          label: i.name,
        }));

        this.findFormControl(this.profileItems, 'gradeId').options =
          this.gradeOptions;
      },
    });
  }

  private loadCreateUserRoleOptions(): void {
    this.nguoiDungService.getCreateUserRoleOptions().subscribe({
      next: ({ data }) => {
        this.findFormControl(this.profileItems, 'roleId').options = (
          data ?? []
        ).map((item) => ({
          value: item.id,
          label: item.name,
        }));
      },
      error: () => {
        this.findFormControl(this.profileItems, 'roleId').options = [];
      },
    });
  }

  private loadProvinces(): void {
    this.diaChiHanhChinhService.getProvinces().subscribe((result) => {
      this.provinceOptions = (result.provinces ?? []).map((item) => {
        this.provinceLookup.set(item.code, item);
        return {
          value: item.code,
          label:
            `${item.administrativeLevel ?? item.type ?? ''} ${item.name}`.trim(),
        };
      });
      this.permanentProvinceItem.options = this.provinceOptions;
      this.temporaryProvinceItem.options = this.provinceOptions;
      this.birthPlaceProvinceItem.options = this.provinceOptions;
    });
  }

  private loadWardOptions(
    provinceCode: string,
    item: FormType,
    wardControlName: string,
    lookup: Map<string, DiaChiPhuongXaItem>,
    selectedWardName?: string
  ): void {
    const wardControl = this.form.get(wardControlName);
    wardControl?.setValue(null, { emitEvent: false });
    if (!provinceCode) {
      item.options = [];
      item.disabled = true;
      lookup.clear();
      wardControl?.disable({ emitEvent: false });
      return;
    }
    item.disabled = false;
    wardControl?.enable({ emitEvent: false });
    this.diaChiHanhChinhService
      .getCommunesByProvince(provinceCode)
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(({ communes }) => {
        lookup.clear();
        item.options = (communes ?? []).map((w) => {
          lookup.set(w.code, w);
          return {
            value: w.code,
            label: `${w.administrativeLevel ?? w.type ?? ''} ${w.name}`.trim(),
          };
        });
        if (selectedWardName) {
          wardControl?.setValue(
            this.getWardCode(selectedWardName, communes ?? []),
            { emitEvent: false }
          );
        }
      });
  }

  private getUnitLabel(unitId?: string | number | null): string {
    if (unitId === null || unitId === undefined || unitId === '') return '';
    return (
      this.unitOptions.find((o) => `${o.value}` === `${unitId}`)?.label ??
      `${unitId}`
    );
  }

  private getGradeLabel(gradeId?: string | number | null): string {
    if (gradeId === null || gradeId === undefined || gradeId === '') return '';
    return (
      this.gradeOptions.find((o) => `${o.value}` === `${gradeId}`)?.label ??
      `${gradeId}`
    );
  }

  // ════════════════════════════════════════
  //  Form patching / payload
  // ════════════════════════════════════════
  private patchForm(d: CanBoDetailResponse): void {
    this.form.patchValue(
      {
        unitId: d.unitId ?? '',
        staffCode: d.staffCode ?? '',
        fullName: d.fullName ?? '',
        aliasName: d.aliasName ?? '',
        identityCode: d.identityCode ?? '',
        gender: this.normalizeGenderValue(d.gender),
        dateOfBirth: this.toInputDate(d.dateOfBirth),
        hometown: d.hometown ?? '',
        permanentAddress: d.permanentAddress?.detailAddress ?? '',
        temporaryAddress: d.temporaryAddress?.detailAddress ?? '',
        birthPlace: d.birthPlaceAddress?.detailAddress ?? '',
        ethnicityId: d.ethnicityId ?? '',
        religionId: d.religionId ?? '',
        nationalityId: d.nationalityId ?? '',
        cccdNo: d.cccdNo ?? '',
        cccdIssueDate: this.toInputDate(d.cccdIssueDate),
        cccdIssuePlace: d.cccdIssuePlace ?? '',
        phone: d.phone ?? '',
        email: d.email ?? '',
        healthStatus: d.healthStatus ?? '',
        gradeId: d.gradeId ?? '',
        socialInsuranceNo: d.socialInsuranceNo ?? '',
        status: d.status ?? 'ACTIVE',
        note: d.note ?? '',
        avatarUrl: d.avatarUrl ?? '',
        // Father
        fatherName: d.fatherInfo?.fullName ?? '',
        fatherBirthYear: d.fatherInfo?.birthYear ?? '',
        fatherBirthPlace: d.fatherInfo?.placeOfBirth ?? '',
        fatherHometown: d.fatherInfo?.hometown ?? '',
        fatherOccupation: d.fatherInfo?.occupation ?? '',
        fatherPhone: d.fatherInfo?.phone ?? '',
        // Mother
        motherName: d.motherInfo?.fullName ?? '',
        motherBirthYear: d.motherInfo?.birthYear ?? '',
        motherBirthPlace: d.motherInfo?.placeOfBirth ?? '',
        motherHometown: d.motherInfo?.hometown ?? '',
        motherOccupation: d.motherInfo?.occupation ?? '',
        motherPhone: d.motherInfo?.phone ?? '',
        // Spouse
        spouseName: d.spouseInfo?.fullName ?? '',
        spouseBirthYear: d.spouseInfo?.birthYear ?? '',
        spouseBirthPlace: d.spouseInfo?.placeOfBirth ?? '',
        spouseHometown: d.spouseInfo?.hometown ?? '',
        spouseOccupation: d.spouseInfo?.occupation ?? '',
        spousePhone: d.spouseInfo?.phone ?? '',
        // Spouse's father
        spouseFatherName: d.spouseFatherInfo?.fullName ?? '',
        spouseFatherBirthYear: d.spouseFatherInfo?.birthYear ?? '',
        spouseFatherBirthPlace: d.spouseFatherInfo?.placeOfBirth ?? '',
        spouseFatherHometown: d.spouseFatherInfo?.hometown ?? '',
        spouseFatherOccupation: d.spouseFatherInfo?.occupation ?? '',
        spouseFatherPhone: d.spouseFatherInfo?.phone ?? '',
        // Mother-in-law
        motherInLawName: d.spouseMotherInfo?.fullName ?? '',
        motherInLawBirthYear: d.spouseMotherInfo?.birthYear ?? '',
        motherInLawBirthPlace: d.spouseMotherInfo?.placeOfBirth ?? '',
        motherInLawHometown: d.spouseMotherInfo?.hometown ?? '',
        motherInLawOccupation: d.spouseMotherInfo?.occupation ?? '',
        motherInLawPhone: d.spouseMotherInfo?.phone ?? '',
        // Children
        childrenInfo: d.childrenDetail ?? '',
      },
      { emitEvent: false }
    );

    // Address province/ward selects - extracting IDs from objects
    this.patchAddressBlock(
      d.permanentAddress?.provinceName,
      'permanentProvinceName',
      this.permanentWardItem,
      'permanentWardName',
      this.permanentWardLookup,
      d.permanentAddress?.wardName
    );
    this.patchAddressBlock(
      d.temporaryAddress?.provinceName,
      'temporaryProvinceName',
      this.temporaryWardItem,
      'temporaryWardName',
      this.temporaryWardLookup,
      d.temporaryAddress?.wardName
    );
    this.patchAddressBlock(
      d.birthPlaceAddress?.provinceName,
      'birthPlaceProvinceName',
      this.birthPlaceWardItem,
      'birthPlaceWardName',
      this.birthPlaceWardLookup,
      d.birthPlaceAddress?.wardName
    );
  }

  private patchAddressBlock(
    provinceName: string | undefined,
    provinceControlName: string,
    wardItem: FormType,
    wardControlName: string,
    wardLookup: Map<string, DiaChiPhuongXaItem>,
    wardName: string | undefined
  ): void {
    const provinceCode = this.getProvinceCode(provinceName);
    if (provinceCode) {
      this.form
        .get(provinceControlName)
        ?.setValue(provinceCode, { emitEvent: false });
      this.loadWardOptions(
        provinceCode,
        wardItem,
        wardControlName,
        wardLookup,
        wardName
      );
    }
  }

  private buildPayload(): CanBoFormRequest {
    const v = this.form.getRawValue();
    return {
      staffCode: v.staffCode,
      fullName: v.fullName ?? '',
      unitId: v.unitId ?? 0,
      aliasName: v.aliasName ?? '',
      identityCode: v.identityCode ?? '',
      gender: v.gender || null,
      dateOfBirth: this.toInputDate(v.dateOfBirth),
      ethnicityId: v.ethnicityId || null,
      religionId: v.religionId || null,
      nationalityId: v.nationalityId || null,
      cccdNo: v.cccdNo ?? '',
      cccdIssueDate: this.toInputDate(v.cccdIssueDate),
      cccdIssuePlace: v.cccdIssuePlace ?? '',
      phone: v.phone ?? '',
      email: v.email ?? '',
      healthStatus: v.healthStatus ?? '',
      gradeId: v.gradeId || null,
      socialInsuranceNo: v.socialInsuranceNo ?? '',
      // Note: form doesn't have avatarFileId yet, maybe sending avatarUrl is acceptable or
      // the backend handles both. I'll include avatarFileId if it exists in data.
      avatarFileId: this.staff.avatarFileId ?? 0,
      avatarUrl: v.avatarUrl ?? this.staff.avatarUrl ?? '',
      signatureFileId: this.staff.signatureFileId ?? 0,
      signatureUrl: this.staff.signatureUrl ?? '',
      status: v.status || null,
      note: v.note ?? '',
      permanentAddress: {
        provinceId: v.permanentProvinceName ?? 0,
        districtId: 0,
        wardId: v.permanentWardName ?? 0,
        detailAddress: v.permanentAddress ?? '',
      },
      temporaryAddress: {
        provinceId: v.temporaryProvinceName ?? 0,
        districtId: 0,
        wardId: v.temporaryWardName ?? 0,
        detailAddress: v.temporaryAddress ?? '',
      },
      birthPlaceAddress: {
        provinceId: v.birthPlaceProvinceName ?? 0,
        districtId: 0,
        wardId: v.birthPlaceWardName ?? 0,
        detailAddress: v.birthPlace ?? '',
      },
      fatherInfo: this.buildFamilyInfo(
        v.fatherName,
        v.fatherBirthYear,
        v.fatherBirthPlace,
        v.fatherHometown,
        v.fatherOccupation,
        v.fatherPhone
      ),
      motherInfo: this.buildFamilyInfo(
        v.motherName,
        v.motherBirthYear,
        v.motherBirthPlace,
        v.motherHometown,
        v.motherOccupation,
        v.motherPhone
      ),
      spouseInfo: this.buildFamilyInfo(
        v.spouseName,
        v.spouseBirthYear,
        v.spouseBirthPlace,
        v.spouseHometown,
        v.spouseOccupation,
        v.spousePhone
      ),
      spouseFatherInfo: this.buildFamilyInfo(
        v.spouseFatherName,
        v.spouseFatherBirthYear,
        v.spouseFatherBirthPlace,
        v.spouseFatherHometown,
        v.spouseFatherOccupation,
        v.spouseFatherPhone
      ),
      spouseMotherInfo: this.buildFamilyInfo(
        v.spouseMotherName,
        v.spouseMotherBirthYear,
        v.spouseMotherBirthPlace,
        v.spouseMotherHometown,
        v.spouseMotherOccupation,
        v.spouseMotherPhone
      ),
      childrenDetail: v.childrenInfo ?? '',
      // Account info
      ...(v.createAccount
        ? {
            accountInfo: {
              username: v.username || null,
              password: v.password || null,
              roleId: v.roleId || null,
              status: v.accountStatus || null,
              sendActivationEmail: v.sendActivationEmail || false,
            },
          }
        : {}),
    };
  }

  // ════════════════════════════════════════
  //  Utilities
  // ════════════════════════════════════════
  private normalizeDetail(data: CanBoDetailResponse): CanBoDetailResponse {
    return {
      ...CAN_BO_PROFILE_FALLBACK,
      ...data,
      gender: this.normalizeGenderValue(data.gender),
      dateOfBirth: this.formatDate(data.dateOfBirth),
      cccdIssueDate: this.formatDate(data.cccdIssueDate),
      gradeName: data.gradeName ?? this.getGradeLabel(data.gradeId),
    };
  }

  private normalizeGenderValue(value?: string | null): string {
    const n = `${value ?? ''}`.trim().toUpperCase();
    if (n === 'MALE' || n === 'NAM' || n === '0') return 'MALE';
    if (n === 'FEMALE' || n === 'NU' || n === '1') return 'FEMALE';
    return n;
  }

  private buildFamilyInfo(
    fullName?: string,
    birthYear?: number,
    placeOfBirth?: string,
    hometown?: string,
    occupation?: string,
    phone?: string
  ):
    | {
        fullName: string;
        birthYear: number;
        placeOfBirth: string;
        hometown: string;
        occupation: string;
        phone: string;
      }
    | undefined {
    // Check if any meaningful data exists (not mock data)
    const hasData =
      (fullName && fullName.trim() && !this.isMockData(fullName)) ||
      (birthYear && birthYear > 0) ||
      (placeOfBirth && placeOfBirth.trim()) ||
      (hometown && hometown.trim()) ||
      (occupation && occupation.trim()) ||
      (phone && phone.trim());

    if (!hasData) return undefined;

    return {
      fullName: fullName ?? '',
      birthYear: birthYear ?? 0,
      placeOfBirth: placeOfBirth ?? '',
      hometown: hometown ?? '',
      occupation: occupation ?? '',
      phone: phone ?? '',
    };
  }

  private isMockData(name?: string): boolean {
    const mockNames = [
      'FATHER',
      'MOTHER',
      'SPOUSE',
      'SPOUSE_FATHER',
      'SPOUSE_MOTHER',
    ];
    return mockNames.includes(name?.trim() ?? '');
  }

  private formatDate(value?: string | null): string {
    const raw = `${value ?? ''}`.trim();
    if (!raw) return '';
    const date = raw.slice(0, 10);
    const [y, m, d] = date.split('-');
    if (!y || !m || !d) return raw;
    return `${d}/${m}/${y}`;
  }

  private toInputDate(value?: string | null): string {
    const raw = `${value ?? ''}`.trim();
    if (!raw) return '';
    if (raw.includes('/')) {
      const [d, m, y] = raw.split('/');
      if (d && m && y)
        return `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`;
    }
    return raw.slice(0, 10);
  }

  private resolveAvatarUrl(value: unknown): string {
    const raw = `${value ?? ''}`.trim();
    if (!raw) return '';
    if (/^https?:\/\//i.test(raw) || raw.startsWith('data:')) return raw;
    const apiHost = `${environment.host_api ?? ''}`.trim();
    if (!raw.startsWith('/')) return raw;
    if (apiHost.startsWith('/')) {
      const apiPrefix = apiHost.replace(/\/$/, '');
      return raw.startsWith(`${apiPrefix}/`) ? raw : `${apiPrefix}${raw}`;
    }
    const absoluteApiBase = apiHost.replace(/\/$/, '');
    const origin = absoluteApiBase.replace(/\/api$/i, '');
    return raw.startsWith('/uploads/')
      ? `${absoluteApiBase}${raw}`
      : `${origin}${raw}`;
  }

  private resolveProvinceName(value: unknown): string {
    const code = `${value ?? ''}`.trim();
    if (!code) return '';
    return this.provinceLookup.get(code)?.name ?? code;
  }

  private resolveWardName(
    value: unknown,
    lookup: Map<string, DiaChiPhuongXaItem>
  ): string {
    const code = `${value ?? ''}`.trim();
    if (!code) return '';
    return lookup.get(code)?.name ?? code;
  }

  private getProvinceCode(name?: string): string | null {
    const n = `${name ?? ''}`.trim();
    if (!n) return null;
    if (this.provinceLookup.has(n)) return n;
    for (const [code, province] of this.provinceLookup.entries()) {
      if (`${province.name ?? ''}`.trim() === n) return code;
    }
    return null;
  }

  private getWardCode(
    name: string,
    wards: DiaChiPhuongXaItem[]
  ): string | null {
    const n = `${name ?? ''}`.trim();
    if (!n) return null;
    const matched = wards.find(
      (w) => w.code === n || `${w.name ?? ''}`.trim() === n
    );
    return matched?.code ?? null;
  }
}
