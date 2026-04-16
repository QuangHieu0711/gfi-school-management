import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { MtxGridColumn } from '@ng-matero/extensions/grid';
import { filter, takeUntil } from 'rxjs';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { TYPE_FORM, TYPE_FORM_KEY } from '@constant/constant';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import {
  DATE_CONTROL,
  FormType,
  IOptions,
  SELECT_CONTROL,
  TEXT_CONTROL,
  TEXTAREA_CONTROL,
} from '@model/form-control.model';
import { COMMON_TABLE_KEY, TableQueryEvent } from '@model/table.model';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';

import { CanBoService } from '@app/service/admin/can-bo.service';
import { DonViService } from '@app/service/admin/don-vi.service';
import { StaffTrainingService } from '@app/service/admin/dao-tao-can-bo.service';
import { StaffForeignLanguageService } from '@app/service/admin/thong-tin-ngoai-ngu-can-bo.service';
import { StaffJobHistoryService } from '@app/service/admin/qua-trinh-cong-tac.service';
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
import { DialogThongTinDaoTaoComponent } from './thong-tin-dao-tao/dialog-thong-tin-dao-tao.component';
import { DialogThongTinNgoaiNguComponent } from './thong-tin-ngoai-ngu/dialog-thong-tin-ngoai-ngu.component';
import { DialogQuaTrinhCongTacComponent } from './qua-trinh-cong-tac/dialog-qua-trinh-cong-tac.component';

type TabKey =
  | 'thong-tin-can-bo'
  | 'qua-trinh-cong-tac'
  | 'thong-tin-dao-tao'
  | 'thong-tin-luong'
  | 'thong-tin-ngoai-ngu';

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

  readonly tabs: Array<{ key: TabKey; label: string }> = [
    { key: 'thong-tin-can-bo', label: 'THÔNG TIN CÁN BỘ' },
    {
      key: 'qua-trinh-cong-tac',
      label: 'QUẢN LÝ QUÁ TRÌNH CÔNG TÁC CỦA CÁN BỘ',
    },
    { key: 'thong-tin-dao-tao', label: 'THÔNG TIN ĐÀO TẠO' },
    { key: 'thong-tin-luong', label: 'THÔNG TIN LƯƠNG' },
    { key: 'thong-tin-ngoai-ngu', label: 'THÔNG TIN NGOẠI NGỮ' },
  ];
  readonly genderOptions = CAN_BO_GENDER_OPTIONS;
  readonly statusOptions = CAN_BO_STATUS_OPTIONS;
  readonly jobHistoryTableConfig = { hasFilterPanel: false };
  readonly trainingTableConfig = { hasFilterPanel: false };
  readonly foreignLanguageTableConfig = { hasFilterPanel: false };

  activeTab: TabKey = 'thong-tin-can-bo';
  staffId?: string;
  staff: CanBoDetailResponse = { ...CAN_BO_PROFILE_FALLBACK };
  unitOptions: IOptions[] = [];
  profileItems: FormType[] = [];
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

  constructor(
    protected override injector: Injector,
    private readonly routeService: ActivatedRoute,
    private readonly routerService: Router,
    private readonly locationService: Location,
    private readonly canBoService: CanBoService,
    private readonly donViService: DonViService,
    private readonly staffJobHistoryService: StaffJobHistoryService,
    private readonly staffTrainingService: StaffTrainingService,
    private readonly staffForeignLanguageService: StaffForeignLanguageService
  ) {
    super(injector);
  }

  get isDetailMode(): boolean {
    return this.pathType === this.TYPE_FORM.DETAIL;
  }

  get isUpdateMode(): boolean {
    return this.pathType === this.TYPE_FORM.UPDATE;
  }

  get visibleTabs(): Array<{ key: TabKey; label: string }> {
    return this.tabs.filter((tab) => tab.key !== ('thong-tin-luong' as TabKey));
  }

  protected override componentInit(): void {
    this.syncPathType();
    this.staffId = this.routeService.snapshot.paramMap.get('id') ?? undefined;
    this.initItems();
    this.initForm();
    this.initJobHistoryColumns();
    this.initTrainingColumns();
    this.initForeignLanguageColumns();
    this.loadUnitOptions();
    this.bindGenerateCode();
    this.bindRouteMode();

    const routeState = history.state?.staff as CanBoDetailResponse | undefined;
    if (routeState) {
      this.staff = this.normalizeDetail(routeState);
      this.patchForm(this.staff);
    }

    if (!this.staffId) return;

    this.canBoService.getById(this.staffId).subscribe({
      next: ({ data }) => {
        this.staff = this.normalizeDetail(data);
        this.patchForm(this.staff);
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

  selectTab(tabKey: TabKey): void {
    this.activeTab = tabKey;
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
    if (!this.staffId || !this.isUpdateMode) return;

    const payload = this.buildPayload();
    this.canBoService.update(this.staffId, payload).subscribe({
      next: () => {
        this.toastr.success('Cập nhật thành công', 'Thành công');
        this.pathType = this.TYPE_FORM.DETAIL;
        this.routerService.navigate(
          [
            '/',
            NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
            ...NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH.split('/'),
            PATH.CHI_TIET,
            this.staffId,
          ],
          {
            state: {
              staff: {
                ...this.staff,
                ...payload,
                staffCode: this.form.get('staffCode')?.value,
              },
            },
          }
        );
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu thất bại',
          'Thất bại'
        );
      },
    });
  }

  getValue(value: unknown): string {
    if (value === null || value === undefined || value === '') return '—';
    return `${value}`;
  }

  getGenderLabel(value?: string | null): string {
    const normalized = `${value ?? ''}`.trim().toUpperCase();
    if (!normalized) return '—';
    if (normalized === 'MALE' || normalized === 'NAM' || normalized === '0') {
      return 'Nam';
    }
    if (normalized === 'FEMALE' || normalized === 'NU' || normalized === '1') {
      return 'Nữ';
    }
    return `${value ?? ''}`;
  }

  getStatusLabel(value?: string | null): string {
    const normalized = `${value ?? ''}`.trim().toUpperCase();
    if (!normalized) return '—';
    return (
      this.statusOptions.find(
        (item) => `${item.value}`.toUpperCase() === normalized
      )?.label ?? `${value ?? ''}`
    );
  }

  loadJobHistories(query?: TableQueryEvent): void {
    if (!this.staffId) return;

    this.jobHistoryPageIndex = query?.pageIndex ?? this.jobHistoryPageIndex;
    this.jobHistoryPageSize = query?.pageSize ?? this.jobHistoryPageSize;

    this.staffJobHistoryService
      .filter({
        pageNow: this.jobHistoryPageIndex + 1,
        pageSize: this.jobHistoryPageSize,
        filter: {
          staffId: Number(this.staffId),
        },
      })
      .subscribe({
        next: ({ data }) => {
          const items = Array.isArray(data)
            ? data
            : Array.isArray(data?.items)
              ? data.items
              : [];

          this.jobHistoryDataSource = items.map((item) => ({
            ...item,
            unitName: this.getUnitLabel(item.unitId),
          }));
          this.jobHistoryTotal = Array.isArray(data)
            ? items.length
            : (data?.recordTotal ?? items.length);
        },
        error: (error) => {
          this.jobHistoryDataSource = [];
          this.jobHistoryTotal = 0;
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Tải quá trình công tác thất bại',
            'Thất bại'
          );
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
        filter: {
          staffId: Number(this.staffId),
        },
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
        error: (error) => {
          this.trainingDataSource = [];
          this.trainingTotal = 0;
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Tải thông tin đào tạo thất bại',
            'Thất bại'
          );
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
        filter: {
          staffId: Number(this.staffId),
        },
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
        error: (error) => {
          this.foreignLanguageDataSource = [];
          this.foreignLanguageTotal = 0;
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Tải thông tin ngoại ngữ thất bại',
            'Thất bại'
          );
        },
      });
  }

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
        if (result) {
          this.loadJobHistories({
            pageIndex: this.jobHistoryPageIndex,
            pageSize: this.jobHistoryPageSize,
          });
        }
      }
    );
  }

  deleteJobHistory(rowData: StaffJobHistoryResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa quá trình công tác ${rowData.decisionNo ?? ''} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.staffJobHistoryService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadJobHistories({
              pageIndex: this.jobHistoryPageIndex,
              pageSize: this.jobHistoryPageSize,
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
        if (result) {
          this.loadTrainingInfos({
            pageIndex: this.trainingPageIndex,
            pageSize: this.trainingPageSize,
          });
        }
      }
    );
  }

  deleteTraining(rowData: StaffTrainingResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa thông tin đào tạo ${rowData.schoolName ?? ''} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.staffTrainingService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadTrainingInfos({
              pageIndex: this.trainingPageIndex,
              pageSize: this.trainingPageSize,
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
        if (result) {
          this.loadForeignLanguages({
            pageIndex: this.foreignLanguagePageIndex,
            pageSize: this.foreignLanguagePageSize,
          });
        }
      }
    );
  }

  deleteForeignLanguage(rowData: StaffForeignLanguageResponse): void {
    this.dialog.confirm(
      {
        title: 'Xác nhận',
        message: `Bạn có chắc chắn muốn xóa thông tin ngoại ngữ ${rowData.languageName ?? ''} không?`,
      },
      (confirmed?: boolean) => {
        if (!confirmed) return;

        this.staffForeignLanguageService.delete(rowData.id).subscribe({
          next: () => {
            this.toastr.success('Xóa thành công', 'Thành công');
            this.loadForeignLanguages({
              pageIndex: this.foreignLanguagePageIndex,
              pageSize: this.foreignLanguagePageSize,
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

  private initItems(): void {
    this.profileItems = [
      TEXT_CONTROL({
        controlName: 'unitId',
        label: 'Đơn vị',
        placeholder: 'Đơn vị',
        required: false,
        disabled: true,
        hidden: true,
      }),
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
        controlName: 'birthPlace',
        label: 'Nơi sinh',
        placeholder: 'Nơi sinh',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'hometown',
        label: 'Quê quán',
        placeholder: 'Quê quán',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'permanentAddress',
        label: 'Thường trú',
        placeholder: 'Thường trú',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'temporaryAddress',
        label: 'Tạm trú',
        placeholder: 'Tạm trú',
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
        controlName: 'healthStatus',
        label: 'Tình trạng sức khỏe',
        placeholder: 'Tình trạng sức khỏe',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'socialInsuranceNo',
        label: 'Số BHXH',
        placeholder: 'Số BHXH',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'fatherName',
        label: 'Cha',
        placeholder: 'Cha',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherName',
        label: 'Mẹ',
        placeholder: 'Mẹ',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseName',
        label: 'Vợ/Chồng',
        placeholder: 'Vợ/Chồng',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'spouseFatherName',
        label: 'Bố chồng/vợ',
        placeholder: 'Bố chồng/vợ',
        required: false,
      }),
      TEXT_CONTROL({
        controlName: 'motherInLawName',
        label: 'Mẹ chồng/vợ',
        placeholder: 'Mẹ chồng/vợ',
        required: false,
      }),
      TEXTAREA_CONTROL({
        controlName: 'childrenInfo',
        label: 'Thông tin con',
        placeholder: 'Thông tin con',
        required: false,
        rows: 4,
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
    ];
  }

  private initForm(): void {
    this.form = this.itemControl.toFormGroup(this.profileItems);
  }

  private initJobHistoryColumns(): void {
    this.jobHistoryColumns = [
      { header: 'STT', field: COMMON_TABLE_KEY.STT, class: 'text-center' },
      { header: 'Từ ngày', field: STAFF_JOB_HISTORY_KEY.FROM_DATE },
      { header: 'Đến ngày', field: STAFF_JOB_HISTORY_KEY.TO_DATE },
      { header: 'Đơn vị', field: 'unitName' },
      { header: 'Phòng ban', field: STAFF_JOB_HISTORY_KEY.DEPARTMENT_ID },
      {
        header: 'Vị trí việc làm',
        field: STAFF_JOB_HISTORY_KEY.WORKING_POSITION_ID,
      },
      { header: 'Chức danh', field: STAFF_JOB_HISTORY_KEY.TITLE_ID },
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
            tooltip: 'Chỉnh sửa',
            click: (rowData: StaffJobHistoryResponse) =>
              this.openJobHistoryDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            tooltip: 'Xóa',
            click: (rowData: StaffJobHistoryResponse) =>
              this.deleteJobHistory(rowData),
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
            tooltip: 'Chỉnh sửa',
            click: (rowData: StaffTrainingResponse) =>
              this.openTrainingDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            tooltip: 'Xóa',
            click: (rowData: StaffTrainingResponse) =>
              this.deleteTraining(rowData),
          },
        ],
      },
    ];
  }

  private initForeignLanguageColumns(): void {
    this.foreignLanguageColumns = [
      { header: 'STT', field: COMMON_TABLE_KEY.STT, class: 'text-center' },
      {
        header: 'Ngoại ngữ',
        field: STAFF_FOREIGN_LANGUAGE_KEY.LANGUAGE_NAME,
      },
      {
        header: 'Trình độ',
        field: STAFF_FOREIGN_LANGUAGE_KEY.LANGUAGE_LEVEL,
      },
      {
        header: 'Ngày cấp',
        field: STAFF_FOREIGN_LANGUAGE_KEY.ISSUE_DATE,
      },
      {
        header: 'Điểm số',
        field: STAFF_FOREIGN_LANGUAGE_KEY.SCORE,
      },
      {
        header: 'Ghi chú',
        field: STAFF_FOREIGN_LANGUAGE_KEY.NOTE,
      },
      {
        header: 'Hành động',
        field: COMMON_TABLE_KEY.ACTION,
        type: 'button',
        class: 'text-center',
        buttons: [
          {
            type: 'icon',
            icon: 'edit',
            tooltip: 'Chỉnh sửa',
            click: (rowData: StaffForeignLanguageResponse) =>
              this.openForeignLanguageDialog(this.TYPE_FORM.UPDATE, rowData),
          },
          {
            type: 'icon',
            icon: 'delete',
            tooltip: 'Xóa',
            click: (rowData: StaffForeignLanguageResponse) =>
              this.deleteForeignLanguage(rowData),
          },
        ],
      },
    ];
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
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.ngUnsubscribe)
      )
      .subscribe(() => {
        this.syncPathType();
      });
  }

  private syncPathType(): void {
    this.getTypeByPath();
  }

  private loadUnitOptions(): void {
    this.donViService.getOptions().subscribe({
      next: ({ data }) => {
        this.unitOptions = (data ?? []).map((item) => ({
          value: item.id,
          label: item.name,
        }));
      },
    });
  }

  private getUnitLabel(unitId?: string | number | null): string {
    if (unitId === null || unitId === undefined || unitId === '') return '';
    return (
      this.unitOptions.find((item) => `${item.value}` === `${unitId}`)?.label ??
      `${unitId}`
    );
  }

  private patchForm(data: CanBoDetailResponse): void {
    this.form.patchValue(
      {
        unitId: data.unitId ?? '',
        staffCode: data.staffCode ?? '',
        fullName: data.fullName ?? '',
        aliasName: data.aliasName ?? '',
        identityCode: data.identityCode ?? '',
        gender: this.normalizeGenderValue(data.gender),
        dateOfBirth: this.toInputDate(data.dateOfBirth),
        birthPlace: data.birthPlace ?? '',
        hometown: data.hometown ?? '',
        permanentAddress: data.permanentAddress ?? '',
        temporaryAddress: data.temporaryAddress ?? '',
        ethnicityId: data.ethnicityId ?? '',
        religionId: data.religionId ?? '',
        nationalityId: data.nationalityId ?? '',
        cccdNo: data.cccdNo ?? '',
        cccdIssueDate: this.toInputDate(data.cccdIssueDate),
        cccdIssuePlace: data.cccdIssuePlace ?? '',
        phone: data.phone ?? '',
        email: data.email ?? '',
        healthStatus: data.healthStatus ?? '',
        socialInsuranceNo: data.socialInsuranceNo ?? '',
        fatherName: data.fatherName ?? '',
        motherName: data.motherName ?? '',
        spouseName: data.spouseName ?? '',
        spouseFatherName: data.spouseFatherName ?? '',
        motherInLawName: data.motherInLawName ?? '',
        childrenInfo: data.childrenInfo ?? '',
        status: data.status ?? 'ACTIVE',
        note: data.note ?? '',
      },
      { emitEvent: false }
    );
  }

  private buildPayload(): CanBoFormRequest {
    const value = this.form.getRawValue();

    return {
      fullName: value.fullName ?? '',
      aliasName: value.aliasName ?? '',
      identityCode: value.identityCode ?? '',
      gender: this.normalizeGenderValue(value.gender),
      dateOfBirth: this.toInputDate(value.dateOfBirth),
      birthPlace: value.birthPlace ?? '',
      hometown: value.hometown ?? '',
      permanentAddress: value.permanentAddress ?? '',
      temporaryAddress: value.temporaryAddress ?? '',
      ethnicityId: value.ethnicityId ?? '',
      religionId: value.religionId ?? '',
      nationalityId: value.nationalityId ?? '',
      cccdNo: value.cccdNo ?? '',
      cccdIssueDate: this.toInputDate(value.cccdIssueDate),
      cccdIssuePlace: value.cccdIssuePlace ?? '',
      phone: value.phone ?? '',
      email: value.email ?? '',
      healthStatus: value.healthStatus ?? '',
      socialInsuranceNo: value.socialInsuranceNo ?? '',
      fatherName: value.fatherName ?? '',
      motherName: value.motherName ?? '',
      spouseName: value.spouseName ?? '',
      spouseFatherName: value.spouseFatherName ?? '',
      motherInLawName: value.motherInLawName ?? '',
      childrenInfo: value.childrenInfo ?? '',
      status: value.status ?? '',
      note: value.note ?? '',
    };
  }

  private normalizeDetail(data: CanBoDetailResponse): CanBoDetailResponse {
    return {
      ...CAN_BO_PROFILE_FALLBACK,
      ...data,
      gender: this.normalizeGenderValue(data.gender),
      dateOfBirth: this.formatDate(data.dateOfBirth),
      cccdIssueDate: this.formatDate(data.cccdIssueDate),
    };
  }

  private normalizeGenderValue(value?: string | null): string {
    const normalized = `${value ?? ''}`.trim().toUpperCase();
    if (normalized === 'MALE' || normalized === 'NAM' || normalized === '0') {
      return 'MALE';
    }
    if (normalized === 'FEMALE' || normalized === 'NU' || normalized === '1') {
      return 'FEMALE';
    }
    return normalized;
  }

  private formatDate(value?: string | null): string {
    const raw = `${value ?? ''}`.trim();
    if (!raw) return '';
    const date = raw.slice(0, 10);
    const [year, month, day] = date.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  private toInputDate(value?: string | null): string {
    const raw = `${value ?? ''}`.trim();
    if (!raw) return '';
    if (raw.includes('/')) {
      const [day, month, year] = raw.split('/');
      if (day && month && year) {
        return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
      }
    }
    return raw.slice(0, 10);
  }
}
