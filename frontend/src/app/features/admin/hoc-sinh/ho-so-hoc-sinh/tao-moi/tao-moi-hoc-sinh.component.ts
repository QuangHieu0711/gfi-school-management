import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, takeUntil, merge } from 'rxjs';
import { cloneDeep } from 'lodash';
import { HOC_SINH_FORM_ITEM } from '@app/model/admin/hoc-sinh.model';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { environment } from '@env/environment';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import {
  DATE_CONTROL,
  FormType,
  IOptions,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE, IResponse } from '@model/response.model';

import { DAN_TOC_OPTIONS } from '@app/model/admin/dan-toc.model';
import { DonViOptionResponse } from '@app/model/admin/don-vi.model';
import {
  HocSinhDetailResponse,
  HocSinhFormRequest,
} from '@app/model/admin/hoc-sinh.model';
import { LopResponse } from '@app/model/admin/lop.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import {
  DiaChiHanhChinhService,
  DiaChiPhuongXaItem,
  DiaChiTinhThanhResponse,
  DiaChiTinhThanhItem,
} from '@app/service/admin/dia-chi-hanh-chinh.service';
import { DonViService } from '@app/service/admin/don-vi.service';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { NguoiDungService } from '@app/service/admin/nguoi-dung.service';

interface GuardianFormValue {
  fullName: string;
  birthYear: number | string | null;
  occupation: string;
  phone: string;
  email: string;
  identityNumber: string;
  isEthnic: boolean;
}

interface AddressFormValue {
  provinceName: string | null;
  wardName: string | null;
  hamletName: string;
  detailAddress: string;
}

interface EnrollmentFormValue {
  schoolYearId: ID_TYPE | null;
  classId: ID_TYPE | null;
  enrolledAt: string | null;
  status: number | string | null;
  isRepeater: boolean;
  sessionsPerWeek: number | string | null;
  studyMode: number | string | null;
  isBoarding: boolean;
  isTwoSessionsPerDay: boolean;
}

interface ProfileFormValue {
  policyObject: string;
  policyBenefit: string;
  priorityCategory: string;
  studentCategory: string;
  regionCategory: string;
  disabilityType: string;
  disabilityExemptEval: boolean;
  supportTuitionCost: boolean;
  resettlementArea: boolean;
  housingSupport: boolean;
  monthlyAllowance: boolean;
  riceSupport: boolean;
  followsMoeProgram: boolean;
  canSwim: boolean;
  learnsEthnicLanguage: boolean;
  studiedKindergarten5yo: boolean;
  needsVietnameseSupport: boolean;
  hasVietnameseReinforcementMaterial: boolean;
  hasEthnicTeachingAssistant: boolean;
  hasParentInternet: boolean;
  hasParentSmartphone: boolean;
  foreignLanguageProgram: string;
  foreignLanguageCertificate: string;
  informaticsCertificate: string;
  careerOrientation: string;
  vocationalOrientation: string;
  joinedTeamDate: string | null;
  joinedUnionDate: string | null;
  joinedPartyDate: string | null;
  otherSystemCode: string;
  ssoCode: string;
}

interface TaoMoiHocSinhFormValue {
  studentCode: string;
  fullName: string;
  firstName: string;
  moeCode: string;
  dateOfBirth: string | null;
  gender: number | string | null;
  placeOfBirth: string;
  ethnicity: string | null;
  religion: string;
  nationality: string;
  mobilePhone: string;
  email: string;
  avatarUrl: string;
  identityNumber: string;
  identityIssueDate: string | null;
  identityIssuePlace: string;
  healthInsuranceNumber: string;
  bloodGroup: string;
  boardingBook: string;
  admissionDate: string | null;
  studentStatus: number | string | null;
  admissionType: number | string | null;
  unitId: ID_TYPE | null;
  enrollment: EnrollmentFormValue;
  addresses: {
    permanent: AddressFormValue;
    temporary: AddressFormValue;
  };
  guardians: {
    father: GuardianFormValue;
    mother: GuardianFormValue;
  };
  profile: ProfileFormValue;
}

interface InitialHocSinhFormData {
  units: IResponse<DonViOptionResponse[]>;
  schoolYears: IResponse<NamHocOptionResponse[]>;
  provinces: DiaChiTinhThanhResponse;
}

@Component({
  selector: 'tao-moi-hoc-sinh',
  standalone: true,
  templateUrl: './tao-moi-hoc-sinh.component.html',
  styleUrls: ['./tao-moi-hoc-sinh.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconComponent,
    ...MATERIAL_MODULE,
    ...FORM_CONTROL_MODULE,
  ],
})
export class TaoMoiHocSinhComponent extends ComponentBaseAbstract {
  readonly items = cloneDeep(HOC_SINH_FORM_ITEM);
  // ── Section collapse state ──────────────────────────
  collapsedSections = new Set<string>();

  toggleSection(key: string): void {
    if (this.collapsedSections.has(key)) {
      this.collapsedSections.delete(key);
    } else {
      this.collapsedSections.add(key);
    }
  }

  isSectionCollapsed(key: string): boolean {
    return this.collapsedSections.has(key);
  }

  unitOptions: IOptions[] = [];
  schoolYearOptions: IOptions[] = [];
  classOptions: IOptions[] = [];
  provinceOptions: IOptions[] = [];
  private studentId?: string;
  private provinceLookup = new Map<string, DiaChiTinhThanhItem>();
  private permanentWardLookup = new Map<string, DiaChiPhuongXaItem>();
  private temporaryWardLookup = new Map<string, DiaChiPhuongXaItem>();
  selectedAvatarName = '';
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  

  
  
  
  
  
  
  
  
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  readonly genderOptions: IOptions[] = [
    { value: 0, label: 'Nam' },
    { value: 1, label: 'Nữ' },
  ];

  readonly studentStatusOptions: IOptions[] = [
    { value: 0, label: 'Đang học' },
    { value: 1, label: 'Đã chuyển trường' },
    { value: 2, label: 'Tạm nghỉ' },
    { value: 3, label: 'Thôi học' },
  ];

  readonly admissionTypeOptions: IOptions[] = [
    { value: 0, label: 'Xét tuyển' },
    { value: 1, label: 'Thi tuyển' },
  ];

  readonly studyModeOptions: IOptions[] = [
    { value: 0, label: 'Học cả ngày' },
    { value: 1, label: 'Bán trú' },
    { value: 2, label: 'Nội trú' },
  ];

  readonly enrollmentStatusOptions: IOptions[] = [
    { value: 0, label: 'Đang học' },
    { value: 1, label: 'Đã chuyển lớp' },
    { value: 2, label: 'Tạm dừng' },
  ];

  constructor(
    protected override injector: Injector,
    private readonly formBuilder: FormBuilder,
    private readonly hocSinhService: HocSinhService,
    private readonly diaChiHanhChinhService: DiaChiHanhChinhService,
    private readonly donViService: DonViService,
    private readonly namHocService: NamHocService,
    private readonly lopService: LopService,
    private readonly routeService: ActivatedRoute,
    private readonly routerService: Router,
    private readonly locationService: Location,
    private readonly nguoiDungService: NguoiDungService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.getTypeByPath();
    this.studentId = this.routeService.snapshot.paramMap.get('id') ?? undefined;

    this.form = this.formBuilder.group({
      studentCode: ['', [Validators.required]],
      fullName: ['', [Validators.required]],
      firstName: [''],
      moeCode: [''],
      dateOfBirth: ['', [Validators.required]],
      gender: [null],
      placeOfBirth: [''],
      ethnicity: [null],
      religion: [''],
      nationality: [''],
      mobilePhone: [''],
      email: [''],
      avatarUrl: [''],
      identityNumber: [''],
      identityIssueDate: [''],
      identityIssuePlace: [''],
      healthInsuranceNumber: [''],
      bloodGroup: [''],
      boardingBook: [''],
      admissionDate: [''],
      studentStatus: [null],
      admissionType: [null],
      unitId: [null, [Validators.required]],
      enrollment: this.formBuilder.group({
        schoolYearId: [null, [Validators.required]],
        classId: [null, [Validators.required]],
        enrolledAt: [''],
        status: [null],
        isRepeater: [false],
        sessionsPerWeek: [0],
        studyMode: [null],
        isBoarding: [false],
        isTwoSessionsPerDay: [false],
      }),
      addresses: this.formBuilder.group({
        permanent: this.formBuilder.group({
          provinceName: [null],
          wardName: [null],
          hamletName: [''],
          detailAddress: [''],
        }),
        temporary: this.formBuilder.group({
          provinceName: [null],
          wardName: [null],
          hamletName: [''],
          detailAddress: [''],
        }),
      }),
      guardians: this.formBuilder.group({
        father: this.formBuilder.group({
          fullName: [''],
          birthYear: [null],
          occupation: [''],
          phone: [''],
          email: [''],
          identityNumber: [''],
          isEthnic: [false],
        }),
        mother: this.formBuilder.group({
          fullName: [''],
          birthYear: [null],
          occupation: [''],
          phone: [''],
          email: [''],
          identityNumber: [''],
          isEthnic: [false],
        }),
      }),
      profile: this.formBuilder.group({
        policyObject: [''],
        policyBenefit: [''],
        priorityCategory: [''],
        studentCategory: [''],
        regionCategory: [''],
        disabilityType: [''],
        disabilityExemptEval: [false],
        supportTuitionCost: [false],
        resettlementArea: [false],
        housingSupport: [false],
        monthlyAllowance: [false],
        riceSupport: [false],
        followsMoeProgram: [false],
        canSwim: [false],
        learnsEthnicLanguage: [false],
        studiedKindergarten5yo: [false],
        needsVietnameseSupport: [false],
        hasVietnameseReinforcementMaterial: [false],
        hasEthnicTeachingAssistant: [false],
        hasParentInternet: [false],
        hasParentSmartphone: [false],
        foreignLanguageProgram: [''],
        foreignLanguageCertificate: [''],
        informaticsCertificate: [''],
        careerOrientation: [''],
        vocationalOrientation: [''],
        joinedTeamDate: [''],
        joinedUnionDate: [''],
        joinedPartyDate: [''],
        otherSystemCode: [''],
        ssoCode: [''],
      }),
    });

    forkJoin({
      units: this.nguoiDungService.getCreateUserUnitOptions(),
      schoolYears: this.namHocService.getOptions(),
      provinces: this.diaChiHanhChinhService.getProvinces(),
    }).subscribe(
      ({ units, schoolYears, provinces }: InitialHocSinhFormData) => {
        this.unitOptions = (units.data ?? []).map((item) => ({
          value: item.id,
          label: item.name,
        }));
        this.items.genderItem.options = this.genderOptions;
        this.items.studentStatusItem.options = this.studentStatusOptions;
        this.items.admissionTypeItem.options = this.admissionTypeOptions;
        this.items.unitItem.options = this.unitOptions;
        this.schoolYearOptions = (schoolYears.data ?? []).map((item) => ({
          value: item.id,
          label: item.name,
        }));
        this.items.schoolYearItem.options = this.schoolYearOptions;
        this.items.enrollmentStatusItem.options = this.enrollmentStatusOptions;
        this.items.studyModeItem.options = this.studyModeOptions;
        this.items.ethnicityItem.options = DAN_TOC_OPTIONS;
        this.provinceOptions = (provinces.provinces ?? []).map((item) => {
          this.provinceLookup.set(item.code, item);
          return {
            value: item.code,
            label:
              `${item.administrativeLevel ?? item.type ?? ''} ${item.name}`.trim(),
          };
        });
        this.items.permanentProvinceItem.options = this.provinceOptions;
        this.items.temporaryProvinceItem.options = this.provinceOptions;

        if (this.pathType === this.TYPE_FORM.UPDATE && this.studentId) {
          this.loadStudentDetail(this.studentId);
        }
      }
    );

    this.items.classItem.disabled = true;
    this.enrollmentForm.get('classId')?.disable({ emitEvent: false });

    this.bindAddressSelects();
    this.bindClassSelect();
  }

  goBack(): void {
    this.locationService.back();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toastr.warning('Vui lòng nhập đầy đủ các trường thông tin bắt buộc.', 'Cảnh báo');
      return;
    }

    const rawValue = this.form.getRawValue();
    const payload = this.buildPayload(rawValue);

    const isUpdate = this.pathType === this.TYPE_FORM.UPDATE && !!this.studentId;
    const request$ = isUpdate
      ? this.hocSinhService.update(this.studentId!, payload)
      : this.hocSinhService.create(payload);

    request$.subscribe({
      next: ({ data }) => {
        this.toastr.success(
          isUpdate ? 'Cập nhật thành công' : 'Thêm học sinh thành công',
          'Thành công'
        );
        if (isUpdate && this.studentId) {
          // Cập nhật → xem chi tiết
          this.routerService.navigate([
            '/',
            NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
            NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
            PATH.CHI_TIET,
            this.studentId,
          ]);
        } else {
          // Thêm mới → về danh sách
          this.routerService.navigate([
            '/',
            NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
            NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
          ]);
        }
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ?? error?.error?.message ?? 'Lưu thất bại',
          'Thất bại'
        );
      },
    });
  }

  get avatarPreview(): string {
    return this.resolveAvatarUrl(this.form?.get('avatarUrl')?.value);
  }

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

  get enrollmentForm(): FormGroup {
    return this.form.get('enrollment') as FormGroup;
  }

  get profileForm(): FormGroup {
    return this.form.get('profile') as FormGroup;
  }

  get fatherForm(): FormGroup {
    return this.form.get(['guardians', 'father']) as FormGroup;
  }

  get motherForm(): FormGroup {
    return this.form.get(['guardians', 'mother']) as FormGroup;
  }

  get permanentAddressForm(): FormGroup {
    return this.form.get(['addresses', 'permanent']) as FormGroup;
  }

  get temporaryAddressForm(): FormGroup {
    return this.form.get(['addresses', 'temporary']) as FormGroup;
  }

  private loadStudentDetail(id: string): void {
    this.hocSinhService.getById(id).subscribe({
      next: ({ data }) => {
        this.patchFormValue(data);
      },
      error: (error) => {
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Tai du lieu that bai',
          'That bai'
        );
      },
    });
  }

  private patchFormValue(data: HocSinhDetailResponse): void {
    const permanentAddress = data.addresses?.find((address) =>
      `${address.addressType ?? ''}`.toLowerCase().includes('thuong')
    );
    const temporaryAddress = data.addresses?.find((address) =>
      `${address.addressType ?? ''}`.toLowerCase().includes('tam')
    );
    const father = data.guardians?.find((guardian) =>
      this.matchesGuardianType(guardian.guardianType, 'CHA', 'FATHER')
    );
    const mother = data.guardians?.find((guardian) =>
      this.matchesGuardianType(guardian.guardianType, 'ME', 'MOTHER')
    );

    this.form.patchValue({
      studentCode: data['studentCode'] ?? '',
      fullName: data['fullName'] ?? '',
      firstName: data['firstName'] ?? '',
      moeCode: data['moeCode'] ?? '',
      dateOfBirth: data['dateOfBirth'] ?? '',
      gender: data['gender'] ?? null,
      placeOfBirth: data['placeOfBirth'] ?? '',
      ethnicity: data['ethnicity'] ?? null,
      religion: data['religion'] ?? '',
      nationality: data['nationality'] ?? '',
      mobilePhone: data['mobilePhone'] ?? '',
      email: data['email'] ?? '',
      avatarUrl: data['avatarUrl'] ?? '',
      identityNumber: data['identityNumber'] ?? '',
      identityIssueDate: data['identityIssueDate'] ?? '',
      identityIssuePlace: data['identityIssuePlace'] ?? '',
      healthInsuranceNumber: data['healthInsuranceNumber'] ?? '',
      bloodGroup: data['bloodGroup'] ?? '',
      boardingBook: data['boardingBook'] ?? '',
      admissionDate: data['admissionDate'] ?? '',
      studentStatus: data['studentStatus'] ?? null,
      admissionType: data['admissionType'] ?? null,
      unitId: data['unitId'] ?? null,
      enrollment: {
        schoolYearId: data.enrollment?.schoolYearId ?? null,
        classId: data.enrollment?.classId ?? null,
        enrolledAt: data.enrollment?.enrolledAt ?? '',
        status: data.enrollment?.status ?? null,
        isRepeater: !!data.enrollment?.isRepeater,
        sessionsPerWeek: data.enrollment?.sessionsPerWeek ?? 0,
        studyMode: data.enrollment?.studyMode ?? null,
        isBoarding: !!data.enrollment?.isBoarding,
        isTwoSessionsPerDay: !!data.enrollment?.isTwoSessionsPerDay,
      },
      guardians: {
        father: {
          fullName: father?.fullName ?? '',
          birthYear: father?.birthYear ?? null,
          occupation: father?.occupation ?? '',
          phone: father?.phone ?? '',
          email: father?.email ?? '',
          identityNumber: father?.identityNumber ?? '',
          isEthnic: !!father?.isEthnic,
        },
        mother: {
          fullName: mother?.fullName ?? '',
          birthYear: mother?.birthYear ?? null,
          occupation: mother?.occupation ?? '',
          phone: mother?.phone ?? '',
          email: mother?.email ?? '',
          identityNumber: mother?.identityNumber ?? '',
          isEthnic: !!mother?.isEthnic,
        },
      },
      profile: {
        policyObject: data.profile?.policyObject ?? '',
        policyBenefit: data.profile?.policyBenefit ?? '',
        priorityCategory: data.profile?.priorityCategory ?? '',
        studentCategory: data.profile?.studentCategory ?? '',
        regionCategory: data.profile?.regionCategory ?? '',
        disabilityType: data.profile?.disabilityType ?? '',
        disabilityExemptEval: !!data.profile?.disabilityExemptEval,
        supportTuitionCost: !!data.profile?.supportTuitionCost,
        resettlementArea: !!data.profile?.resettlementArea,
        housingSupport: !!data.profile?.housingSupport,
        monthlyAllowance: !!data.profile?.monthlyAllowance,
        riceSupport: !!data.profile?.riceSupport,
        followsMoeProgram: !!data.profile?.followsMoeProgram,
        canSwim: !!data.profile?.canSwim,
        learnsEthnicLanguage: !!data.profile?.learnsEthnicLanguage,
        studiedKindergarten5yo: !!data.profile?.studiedKindergarten5yo,
        needsVietnameseSupport: !!data.profile?.needsVietnameseSupport,
        hasVietnameseReinforcementMaterial:
          !!data.profile?.hasVietnameseReinforcementMaterial,
        hasEthnicTeachingAssistant: !!data.profile?.hasEthnicTeachingAssistant,
        hasParentInternet: !!data.profile?.hasParentInternet,
        hasParentSmartphone: !!data.profile?.hasParentSmartphone,
        foreignLanguageProgram: data.profile?.foreignLanguageProgram ?? '',
        foreignLanguageCertificate:
          data.profile?.foreignLanguageCertificate ?? '',
        informaticsCertificate: data.profile?.informaticsCertificate ?? '',
        careerOrientation: data.profile?.careerOrientation ?? '',
        vocationalOrientation: data.profile?.vocationalOrientation ?? '',
        joinedTeamDate: data.profile?.joinedTeamDate ?? '',
        joinedUnionDate: data.profile?.joinedUnionDate ?? '',
        joinedPartyDate: data.profile?.joinedPartyDate ?? '',
        otherSystemCode: data.profile?.otherSystemCode ?? '',
        ssoCode: data.profile?.ssoCode ?? '',
      },
    }, { emitEvent: false });

    this.patchAddressForm(
      this.permanentAddressForm,
      permanentAddress?.provinceName,
      permanentAddress?.wardName,
      permanentAddress?.hamletName,
      permanentAddress?.detailAddress,
      this.items.permanentWardItem,
      this.permanentWardLookup
    );
    this.patchAddressForm(
      this.temporaryAddressForm,
      temporaryAddress?.provinceName,
      temporaryAddress?.wardName,
      temporaryAddress?.hamletName,
      temporaryAddress?.detailAddress,
      this.items.temporaryWardItem,
      this.temporaryWardLookup
    );

    const unitId = data['unitId'] ?? null;
    const schoolYearId = data.enrollment?.schoolYearId ?? null;
    const selectedClassId = data.enrollment?.classId ?? null;
    this.loadClassOptions(unitId, schoolYearId, selectedClassId);
  }

  private patchAddressForm(
    group: FormGroup,
    provinceName?: string,
    wardName?: string,
    hamletName?: string,
    detailAddress?: string,
    wardItem?: FormType,
    lookup?: Map<string, DiaChiPhuongXaItem>
  ): void {
    const provinceCode = this.getProvinceCode(provinceName);
    group.patchValue(
      {
        provinceName: provinceCode,
        wardName: null,
        hamletName: hamletName ?? '',
        detailAddress: detailAddress ?? '',
      },
      { emitEvent: false }
    );

    if (provinceCode && wardItem && lookup) {
      this.loadWardOptions(provinceCode, wardItem, group, lookup, wardName);
    }
  }

  private buildPayload(rawValue: TaoMoiHocSinhFormValue): HocSinhFormRequest {
    return {
      studentCode: rawValue.studentCode || '',
      fullName: rawValue.fullName || '',
      firstName: rawValue.firstName || '',
      moeCode: rawValue.moeCode || '',
      dateOfBirth: rawValue.dateOfBirth || undefined,
      gender: this.numberOrZero(rawValue.gender),
      placeOfBirth: rawValue.placeOfBirth || '',
      ethnicity: rawValue.ethnicity || '',
      religion: rawValue.religion || '',
      nationality: rawValue.nationality || '',
      mobilePhone: rawValue.mobilePhone || '',
      email: rawValue.email || '',
      avatarUrl: rawValue.avatarUrl || '',
      identityNumber: rawValue.identityNumber || '',
      identityIssueDate: rawValue.identityIssueDate || undefined,
      identityIssuePlace: rawValue.identityIssuePlace || '',
      healthInsuranceNumber: rawValue.healthInsuranceNumber || '',
      bloodGroup: rawValue.bloodGroup || '',
      boardingBook: rawValue.boardingBook || '',
      admissionDate: rawValue.admissionDate || undefined,
      studentStatus: this.numberOrZero(rawValue.studentStatus),
      admissionType: this.numberOrZero(rawValue.admissionType),
      unitId: rawValue.unitId ?? undefined,
      enrollment: {
        schoolYearId: rawValue.enrollment.schoolYearId ?? undefined,
        classId: rawValue.enrollment.classId ?? undefined,
        enrolledAt: rawValue.enrollment.enrolledAt || undefined,
        status: this.numberOrZero(rawValue.enrollment.status),
        isRepeater: !!rawValue.enrollment.isRepeater,
        sessionsPerWeek: Number(rawValue.enrollment.sessionsPerWeek || 0),
        studyMode: this.numberOrZero(rawValue.enrollment.studyMode),
        isBoarding: !!rawValue.enrollment.isBoarding,
        isTwoSessionsPerDay: !!rawValue.enrollment.isTwoSessionsPerDay,
      },
      addresses: [
        {
          addressType: 'THUONG_TRU',
          provinceName: this.resolveProvinceName(
            rawValue.addresses.permanent.provinceName
          ),
          wardName: this.resolveWardName(
            rawValue.addresses.permanent.wardName,
            this.permanentWardLookup
          ),
          hamletName: rawValue.addresses.permanent.hamletName || '',
          detailAddress: rawValue.addresses.permanent.detailAddress || '',
        },
        {
          addressType: 'TAM_TRU',
          provinceName: this.resolveProvinceName(
            rawValue.addresses.temporary.provinceName
          ),
          wardName: this.resolveWardName(
            rawValue.addresses.temporary.wardName,
            this.temporaryWardLookup
          ),
          hamletName: rawValue.addresses.temporary.hamletName || '',
          detailAddress: rawValue.addresses.temporary.detailAddress || '',
        },
      ],
      guardians: [
        {
          guardianType: 'CHA',
          fullName: rawValue.guardians.father.fullName || '',
          birthYear: this.toNullableNumber(rawValue.guardians.father.birthYear),
          occupation: rawValue.guardians.father.occupation || '',
          phone: rawValue.guardians.father.phone || '',
          email: rawValue.guardians.father.email || '',
          identityNumber: rawValue.guardians.father.identityNumber || '',
          isEthnic: !!rawValue.guardians.father.isEthnic,
        },
        {
          guardianType: 'ME',
          fullName: rawValue.guardians.mother.fullName || '',
          birthYear: this.toNullableNumber(rawValue.guardians.mother.birthYear),
          occupation: rawValue.guardians.mother.occupation || '',
          phone: rawValue.guardians.mother.phone || '',
          email: rawValue.guardians.mother.email || '',
          identityNumber: rawValue.guardians.mother.identityNumber || '',
          isEthnic: !!rawValue.guardians.mother.isEthnic,
        },
      ],
      profile: {
        ...rawValue.profile,
        joinedTeamDate: rawValue.profile.joinedTeamDate || undefined,
        joinedUnionDate: rawValue.profile.joinedUnionDate || undefined,
        joinedPartyDate: rawValue.profile.joinedPartyDate || undefined,
      },
    };
  }

  private numberOrZero(value: unknown): number {
    return Number(value ?? 0);
  }

  private toNullableNumber(value: unknown): number | undefined {
    if (value === null || value === undefined || value === '') return undefined;
    const numberValue = Number(value);
    return Number.isNaN(numberValue) ? undefined : numberValue;
  }

  private bindAddressSelects(): void {
    this.permanentAddressForm
      .get('provinceName')
      ?.valueChanges.pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((provinceCode) => {
        this.loadWardOptions(
          `${provinceCode ?? ''}`,
          this.items.permanentWardItem,
          this.permanentAddressForm,
          this.permanentWardLookup
        );
      });

    this.temporaryAddressForm
      .get('provinceName')
      ?.valueChanges.pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((provinceCode) => {
        this.loadWardOptions(
          `${provinceCode ?? ''}`,
          this.items.temporaryWardItem,
          this.temporaryAddressForm,
          this.temporaryWardLookup
        );
      });
  }

  private bindClassSelect(): void {
    const unitIdControl = this.form.get('unitId');
    const schoolYearIdControl = this.enrollmentForm.get('schoolYearId');

    merge(
      unitIdControl!.valueChanges,
      schoolYearIdControl!.valueChanges
    ).pipe(takeUntil(this.ngUnsubscribe)).subscribe(() => {
      this.loadClassOptions(unitIdControl?.value, schoolYearIdControl?.value);
    });
  }

  private loadClassOptions(unitId: string | number | null, schoolYearId: string | number | null, selectedClassId?: string | number | null): void {
    const classIdControl = this.enrollmentForm.get('classId');

    if (!selectedClassId) {
      classIdControl?.setValue(null, { emitEvent: false });
    }

    if (!unitId || !schoolYearId) {
      this.items.classItem.options = [];
      this.items.classItem.disabled = true;
      classIdControl?.disable({ emitEvent: false });
      return;
    }

    this.items.classItem.disabled = false;
    classIdControl?.enable({ emitEvent: false });

    this.lopService.getOptions({ unitId, schoolYearId }).pipe(takeUntil(this.ngUnsubscribe)).subscribe({
      next: ({ data }) => {
        this.classOptions = (data ?? []).map((item) => ({
          value: item.id,
          label: item.name,
        }));
        this.items.classItem.options = this.classOptions;
        
        if (selectedClassId) {
          classIdControl?.setValue(selectedClassId, { emitEvent: false });
        }
      }
    });
  }

  private loadWardOptions(
    provinceCode: string,
    item: FormType,
    group: FormGroup,
    lookup: Map<string, DiaChiPhuongXaItem>,
    selectedWardName?: string
  ): void {
    const wardControl = group.get('wardName');
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
        item.options = (communes ?? []).map((ward) => {
          lookup.set(ward.code, ward);
          return {
            value: ward.code,
            label:
              `${ward.administrativeLevel ?? ward.type ?? ''} ${ward.name}`.trim(),
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

  private getProvinceCode(name?: string): string | null {
    const normalized = `${name ?? ''}`.trim();
    if (!normalized) return null;
    if (this.provinceLookup.has(normalized)) return normalized;

    for (const [code, province] of this.provinceLookup.entries()) {
      if (`${province.name ?? ''}`.trim() === normalized) {
        return code;
      }
    }

    return null;
  }

  private getWardCode(
    name: string,
    wards: DiaChiPhuongXaItem[]
  ): string | null {
    const normalized = `${name ?? ''}`.trim();
    if (!normalized) return null;

    const matchedWard = wards.find(
      (ward) =>
        ward.code === normalized || `${ward.name ?? ''}`.trim() === normalized
    );

    return matchedWard?.code ?? null;
  }

  private matchesGuardianType(
    guardianType: unknown,
    ...keywords: string[]
  ): boolean {
    const normalized = `${guardianType ?? ''}`.toUpperCase();
    return keywords.some((keyword) => normalized.includes(keyword));
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
}
