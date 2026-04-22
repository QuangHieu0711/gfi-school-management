/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, takeUntil, merge } from 'rxjs';
import { cloneDeep } from 'lodash';
import {
  HOC_SINH_FORM_ITEM,
  HOC_SINH_BASIC_SUB_FORM,
  HOC_SINH_ENROLLMENT_SUB_FORM,
  HOC_SINH_ADDRESS_SUB_FORM,
  HOC_SINH_GUARDIAN_SUB_FORM,
  HOC_SINH_PROFILE_SUB_FORM,
} from '@app/model/admin/hoc-sinh.model';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { environment } from '@env/environment';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { FormType, IOptions } from '@model/form-control.model';
import { IResponse } from '@model/response.model';

import { DAN_TOC_OPTIONS } from '@app/model/admin/dan-toc.model';
import { DonViOptionResponse } from '@app/model/admin/don-vi.model';
import {
  HocSinhAddress,
  HocSinhDetailResponse,
  HocSinhFormRequest,
} from '@app/model/admin/hoc-sinh.model';
import { NamHocOptionResponse } from '@app/model/admin/nam-hoc.model';
import {
  DiaChiHanhChinhService,
  DiaChiPhuongXaItem,
  DiaChiTinhThanhResponse,
  DiaChiTinhThanhItem,
} from '@app/service/admin/dia-chi-hanh-chinh.service';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';
import { DonViService } from '@app/service/admin/don-vi.service';

interface InitialHocSinhFormData {
  units: IResponse<DonViOptionResponse[]>;
  provinces: DiaChiTinhThanhResponse;
  schoolYears?: IResponse<NamHocOptionResponse[]>;
  currentSchoolYear?: IResponse<NamHocOptionResponse>;
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
  // ── Section collapse state (empty = all sections expanded)
  collapsedSections = new Set<string>();

  toggleSection(key: string): void {
    if (this.collapsedSections.has(key)) {
      this.collapsedSections.delete(key);
    } else {
      this.collapsedSections.add(key);
    }
  }

  onCollapseButtonClick(event: Event, key: string): void {
    event.stopPropagation();
    this.toggleSection(key);
  }

  onAvatarCardKeydown(event: KeyboardEvent, input: HTMLInputElement): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      input.click();
    }
  }

  isSectionCollapsed(key: string): boolean {
    return this.collapsedSections.has(key);
  }

  unitOptions: IOptions[] = [];
  schoolYearOptions: IOptions[] = [];
  classOptions: IOptions[] = [];
  provinceOptions: IOptions[] = [];
  private classOptionsRequestId = 0;

  private studentId?: string;
  private provinceLookup = new Map<string, DiaChiTinhThanhItem>();
  private permanentWardLookup = new Map<string, DiaChiPhuongXaItem>();
  private temporaryWardLookup = new Map<string, DiaChiPhuongXaItem>();
  selectedAvatarName = '';

  constructor(
    protected override injector: Injector,
    private readonly formBuilder: FormBuilder,
    private readonly hocSinhService: HocSinhService,
    private readonly diaChiHanhChinhService: DiaChiHanhChinhService,
    private readonly namHocService: NamHocService,
    private readonly lopService: LopService,
    private readonly routeService: ActivatedRoute,
    private readonly routerService: Router,
    private readonly locationService: Location,
    private readonly donViService: DonViService
  ) {
    super(injector);
  }

  get enrollmentForm(): FormGroup {
    return this.form.get('enrollment') as FormGroup;
  }
  get permanentAddressForm(): FormGroup {
    return this.form.get('addresses.permanent') as FormGroup;
  }
  get temporaryAddressForm(): FormGroup {
    return this.form.get('addresses.temporary') as FormGroup;
  }
  get fatherForm(): FormGroup {
    return this.form.get('guardians.father') as FormGroup;
  }
  get motherForm(): FormGroup {
    return this.form.get('guardians.mother') as FormGroup;
  }
  get fatherIsEthnicControl(): FormControl<boolean> {
    return this.fatherForm.get('isEthnic') as FormControl<boolean>;
  }
  get motherIsEthnicControl(): FormControl<boolean> {
    return this.motherForm.get('isEthnic') as FormControl<boolean>;
  }
  get profileForm(): FormGroup {
    return this.form.get('profile') as FormGroup;
  }
  get avatarPreview(): string {
    return this.resolveAvatarUrl(this.form.get('avatarUrl')?.value);
  }
  get pageTitle(): string {
    return this.pathType === this.TYPE_FORM.UPDATE
      ? 'CHỈNH SỬA HỒ SƠ HỌC SINH'
      : 'THÊM MỚI HỒ SƠ HỌC SINH';
  }

  protected override componentInit(): void {
    this.getTypeByPath();
    this.studentId = this.routeService.snapshot.paramMap.get('id') ?? undefined;

    this.initForm();
    this.loadInitialData();
    this.bindEvents();
  }

  private initForm(): void {
    const it = this.items;
    const basicForm = this.itemControl.toFormGroup([
      ...HOC_SINH_BASIC_SUB_FORM,
      it.avatarUrlItem,
    ]);
    const enrollmentForm = this.itemControl.toFormGroup(
      HOC_SINH_ENROLLMENT_SUB_FORM
    );
    const permanentAddressForm = this.itemControl.toFormGroup(
      HOC_SINH_ADDRESS_SUB_FORM
    );
    const temporaryAddressForm = this.itemControl.toFormGroup([
      it.temporaryProvinceItem,
      it.temporaryWardItem,
      it.temporaryHamletNameItem,
      it.temporaryDetailAddressItem,
    ]);
    const fatherForm = this.itemControl.toFormGroup(HOC_SINH_GUARDIAN_SUB_FORM);
    const motherForm = this.itemControl.toFormGroup([
      it.motherFullNameItem,
      it.motherBirthYearItem,
      it.motherOccupationItem,
      it.motherPhoneItem,
      it.motherEmailItem,
      it.motherIdentityNumberItem,
    ]);
    const profileForm = this.itemControl.toFormGroup(HOC_SINH_PROFILE_SUB_FORM);

    enrollmentForm.addControl(
      'isRepeater',
      new FormControl(false, { nonNullable: true })
    );
    enrollmentForm.addControl(
      'isBoarding',
      new FormControl(false, { nonNullable: true })
    );
    enrollmentForm.addControl(
      'isTwoSessionsPerDay',
      new FormControl(false, { nonNullable: true })
    );
    fatherForm.addControl(
      'isEthnic',
      new FormControl(false, { nonNullable: true })
    );
    motherForm.addControl(
      'isEthnic',
      new FormControl(false, { nonNullable: true })
    );

    [
      'disabilityExemptEval',
      'supportTuitionCost',
      'resettlementArea',
      'housingSupport',
      'monthlyAllowance',
      'riceSupport',
      'followsMoeProgram',
      'canSwim',
      'learnsEthnicLanguage',
      'studiedKindergarten5yo',
      'needsVietnameseSupport',
      'hasVietnameseReinforcementMaterial',
      'hasEthnicTeachingAssistant',
      'hasParentInternet',
      'hasParentSmartphone',
    ].forEach((key) =>
      profileForm.addControl(key, new FormControl(false, { nonNullable: true }))
    );

    this.form = this.formBuilder.group({
      ...basicForm.controls,
      enrollment: enrollmentForm,
      addresses: this.formBuilder.group({
        permanent: permanentAddressForm,
        temporary: temporaryAddressForm,
      }),
      guardians: this.formBuilder.group({
        father: fatherForm,
        mother: motherForm,
      }),
      profile: profileForm,
    });
  }

  private loadInitialData(): void {
    const isUpdate =
      this.pathType === this.TYPE_FORM.UPDATE && !!this.studentId;
    const initialRequest$ = forkJoin({
      units: this.donViService.getCreateUserUnitOptions(),
      provinces: this.diaChiHanhChinhService.getProvinces(),
      ...(isUpdate
        ? { schoolYears: this.namHocService.getOptions() }
        : { currentSchoolYear: this.namHocService.getCurrent() }),
    });

    initialRequest$.subscribe(
      ({
        units,
        schoolYears,
        currentSchoolYear,
        provinces,
      }: InitialHocSinhFormData) => {
        this.unitOptions = (units.data ?? []).map((item) => ({
          value: item.id,
          label: item.name,
        }));
        this.items.unitItem.options = this.unitOptions;

        this.schoolYearOptions = (
          schoolYears?.data
            ? schoolYears.data
            : this.buildCurrentSchoolYearOptions(currentSchoolYear?.data)
        ).map((item) => ({
          value: item.id,
          label: item.name,
        }));
        this.items.schoolYearItem.options = this.schoolYearOptions;

        if (!isUpdate && this.schoolYearOptions.length > 0) {
          const selectedSchoolYearId = this.schoolYearOptions[0].value;
          this.enrollmentForm
            .get('schoolYearId')
            ?.setValue(selectedSchoolYearId, { emitEvent: true });
        }

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

        if (isUpdate && this.studentId) {
          this.loadStudentDetail(this.studentId);
        }
      }
    );
  }

  private buildCurrentSchoolYearOptions(
    currentSchoolYear?: NamHocOptionResponse
  ): NamHocOptionResponse[] {
    return currentSchoolYear ? [currentSchoolYear] : [];
  }

  private bindEvents(): void {
    this.items.classItem.disabled = true;
    this.enrollmentForm.get('classId')?.disable({ emitEvent: false });
    this.bindAddressSelects();
    this.bindClassSelect();
    this.bindGenerateCode();
  }

  goBack(): void {
    this.locationService.back();
  }

  submit(): void {
    const v = this.form.getRawValue();
    const payload = this.buildPayload(v);
    const isUpdate =
      this.pathType === this.TYPE_FORM.UPDATE && !!this.studentId;
    const request$ = isUpdate
      ? this.hocSinhService.update(this.studentId!, payload)
      : this.hocSinhService.create(payload);

    request$.subscribe({
      next: () => {
        this.toastr.success(
          isUpdate ? 'Cập nhật thành công' : 'Thêm học sinh thành công',
          'Thành công'
        );
        this.routerService.navigate([
          '/',
          NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
          NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
        ]);
      },
      error: (err) =>
        this.toastr.error(
          err?.error?.userMessage ?? err?.error?.message ?? 'Lưu thất bại',
          'Thất bại'
        ),
    });
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

  get studentCodeItem() {
    return this.items.studentCodeItem;
  }
  get fullNameItem() {
    return this.items.fullNameItem;
  }
  get firstNameItem() {
    return this.items.firstNameItem;
  }
  get moeCodeItem() {
    return this.items.moeCodeItem;
  }
  get dateOfBirthItem() {
    return this.items.dateOfBirthItem;
  }
  get genderItem() {
    return this.items.genderItem;
  }
  get admissionDateItem() {
    return this.items.admissionDateItem;
  }
  get studentStatusItem() {
    return this.items.studentStatusItem;
  }
  get admissionTypeItem() {
    return this.items.admissionTypeItem;
  }
  get unitItem() {
    return this.items.unitItem;
  }

  // Father Guardian
  get fatherFullNameItem() {
    return this.items.fatherFullNameItem;
  }
  get fatherBirthYearItem() {
    return this.items.fatherBirthYearItem;
  }
  get fatherOccupationItem() {
    return this.items.fatherOccupationItem;
  }
  get fatherPhoneItem() {
    return this.items.fatherPhoneItem;
  }
  get fatherEmailItem() {
    return this.items.fatherEmailItem;
  }
  get fatherIdentityNumberItem() {
    return this.items.fatherIdentityNumberItem;
  }

  // Mother Guardian
  get motherFullNameItem() {
    return this.items.motherFullNameItem;
  }
  get motherBirthYearItem() {
    return this.items.motherBirthYearItem;
  }
  get motherOccupationItem() {
    return this.items.motherOccupationItem;
  }
  get motherPhoneItem() {
    return this.items.motherPhoneItem;
  }
  get motherEmailItem() {
    return this.items.motherEmailItem;
  }
  get motherIdentityNumberItem() {
    return this.items.motherIdentityNumberItem;
  }

  // General Information
  get placeOfBirthItem() {
    return this.items.placeOfBirthItem;
  }
  get ethnicityItem() {
    return this.items.ethnicityItem;
  }
  get religionItem() {
    return this.items.religionItem;
  }
  get nationalityItem() {
    return this.items.nationalityItem;
  }
  get mobilePhoneItem() {
    return this.items.mobilePhoneItem;
  }
  get emailItem() {
    return this.items.emailItem;
  }
  get identityNumberItem() {
    return this.items.identityNumberItem;
  }
  get identityIssueDateItem() {
    return this.items.identityIssueDateItem;
  }
  get identityIssuePlaceItem() {
    return this.items.identityIssuePlaceItem;
  }
  get healthInsuranceNumberItem() {
    return this.items.healthInsuranceNumberItem;
  }
  get bloodGroupItem() {
    return this.items.bloodGroupItem;
  }
  get boardingBookItem() {
    return this.items.boardingBookItem;
  }

  // Enrollment
  get schoolYearItem() {
    return this.items.schoolYearItem;
  }
  get classItem() {
    return this.items.classItem;
  }
  get enrolledAtItem() {
    return this.items.enrolledAtItem;
  }
  get enrollmentStatusItem() {
    return this.items.enrollmentStatusItem;
  }
  get sessionsPerWeekItem() {
    return this.items.sessionsPerWeekItem;
  }
  get studyModeItem() {
    return this.items.studyModeItem;
  }

  // Address
  get permanentProvinceItem() {
    return this.items.permanentProvinceItem;
  }
  get permanentWardItem() {
    return this.items.permanentWardItem;
  }
  get permanentHamletNameItem() {
    return this.items.permanentHamletNameItem;
  }
  get permanentDetailAddressItem() {
    return this.items.permanentDetailAddressItem;
  }
  get temporaryProvinceItem() {
    return this.items.temporaryProvinceItem;
  }
  get temporaryWardItem() {
    return this.items.temporaryWardItem;
  }
  get temporaryHamletNameItem() {
    return this.items.temporaryHamletNameItem;
  }
  get temporaryDetailAddressItem() {
    return this.items.temporaryDetailAddressItem;
  }

  // Profile
  get policyObjectItem() {
    return this.items.policyObjectItem;
  }
  get policyBenefitItem() {
    return this.items.policyBenefitItem;
  }
  get priorityCategoryItem() {
    return this.items.priorityCategoryItem;
  }
  get studentCategoryItem() {
    return this.items.studentCategoryItem;
  }
  get regionCategoryItem() {
    return this.items.regionCategoryItem;
  }
  get disabilityTypeItem() {
    return this.items.disabilityTypeItem;
  }
  get foreignLanguageProgramItem() {
    return this.items.foreignLanguageProgramItem;
  }
  get foreignLanguageCertificateItem() {
    return this.items.foreignLanguageCertificateItem;
  }
  get informaticsCertificateItem() {
    return this.items.informaticsCertificateItem;
  }
  get careerOrientationItem() {
    return this.items.careerOrientationItem;
  }
  get vocationalOrientationItem() {
    return this.items.vocationalOrientationItem;
  }
  get joinedTeamDateItem() {
    return this.items.joinedTeamDateItem;
  }
  get joinedUnionDateItem() {
    return this.items.joinedUnionDateItem;
  }
  get joinedPartyDateItem() {
    return this.items.joinedPartyDateItem;
  }
  get otherSystemCodeItem() {
    return this.items.otherSystemCodeItem;
  }
  get ssoCodeItem() {
    return this.items.ssoCodeItem;
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
            'Tải dữ liệu thất bại',
          'Thất bại'
        );
      },
    });
  }

  private patchFormValue(data: HocSinhDetailResponse): void {
    const { enrollment, profile, addresses, guardians, ...basic } = data;
    this.form.patchValue(basic, { emitEvent: false });
    if (enrollment)
      this.enrollmentForm.patchValue(enrollment, { emitEvent: false });
    if (profile) this.profileForm.patchValue(profile, { emitEvent: false });

    const father = guardians?.find((g) =>
      this.matchesGuardianType(g.guardianType, 'CHA')
    );
    if (father) this.fatherForm.patchValue(father, { emitEvent: false });
    const mother = guardians?.find((g) =>
      this.matchesGuardianType(g.guardianType, 'ME')
    );
    if (mother) this.motherForm.patchValue(mother, { emitEvent: false });

    const perm = addresses?.find((a) => `${a.addressType}`.includes('THUONG'));
    const temp = addresses?.find((a) => `${a.addressType}`.includes('TAM'));

    this.patchAddressSection(
      this.permanentAddressForm,
      perm,
      this.items.permanentWardItem,
      this.permanentWardLookup
    );
    this.patchAddressSection(
      this.temporaryAddressForm,
      temp,
      this.items.temporaryWardItem,
      this.temporaryWardLookup
    );

    this.loadClassOptions(
      data.unitId,
      enrollment?.schoolYearId,
      enrollment?.classId
    );
  }

  private patchAddressSection(
    group: FormGroup,
    addr: HocSinhAddress | undefined,
    item: FormType,
    lookup: Map<string, any>
  ): void {
    const provinceCode = this.getProvinceCode(addr?.provinceName);
    group.patchValue(
      { ...addr, provinceName: provinceCode, wardName: null },
      { emitEvent: false }
    );
    if (provinceCode)
      this.loadWardOptions(provinceCode, item, group, lookup, addr?.wardName);
  }

  private buildPayload(v: any): HocSinhFormRequest {
    return {
      ...v,
      gender: this.numberOrZero(v.gender),
      studentStatus: this.numberOrZero(v.studentStatus),
      admissionType: this.numberOrZero(v.admissionType),
      enrollment: {
        ...v.enrollment,
        status: this.numberOrZero(v.enrollment.status),
        studyMode: this.numberOrZero(v.enrollment.studyMode),
        sessionsPerWeek: Number(v.enrollment.sessionsPerWeek || 0),
      },
      addresses: [
        {
          addressType: 'THUONG_TRU',
          ...v.addresses.permanent,
          provinceName: this.resolveProvinceName(
            v.addresses.permanent.provinceName
          ),
          wardName: this.resolveWardName(
            v.addresses.permanent.wardName,
            this.permanentWardLookup
          ),
        },
        {
          addressType: 'TAM_TRU',
          ...v.addresses.temporary,
          provinceName: this.resolveProvinceName(
            v.addresses.temporary.provinceName
          ),
          wardName: this.resolveWardName(
            v.addresses.temporary.wardName,
            this.temporaryWardLookup
          ),
        },
      ],
      guardians: [
        {
          guardianType: 'CHA',
          ...v.guardians.father,
          birthYear: this.toNullableNumber(v.guardians.father.birthYear),
        },
        {
          guardianType: 'ME',
          ...v.guardians.mother,
          birthYear: this.toNullableNumber(v.guardians.mother.birthYear),
        },
      ],
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
    const classIdControl = this.enrollmentForm.get('classId');

    merge(unitIdControl!.valueChanges, schoolYearIdControl!.valueChanges)
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(() => {
        const unitId = unitIdControl?.value;
        const schoolYearId = schoolYearIdControl?.value;

        if (unitId && schoolYearId) {
          this.loadClassOptions(unitId, schoolYearId);
        } else {
          this.items.classItem.options = [];
          this.items.classItem.disabled = true;
          classIdControl?.disable({ emitEvent: false });
        }
      });
  }

  private bindGenerateCode(): void {
    const unitIdControl = this.form.get('unitId');
    const studentCodeControl = this.form.get('studentCode');

    this.items.studentCodeItem.disabled = true;
    studentCodeControl?.disable({ emitEvent: false });

    if (this.pathType === this.TYPE_FORM.CREATE) {
      unitIdControl?.valueChanges
        .pipe(takeUntil(this.ngUnsubscribe))
        .subscribe((unitId) => {
          if (unitId) {
            this.hocSinhService.generateCode(unitId).subscribe(({ data }) => {
              studentCodeControl?.setValue(data, { emitEvent: false });
            });
          } else {
            studentCodeControl?.setValue('', { emitEvent: false });
          }
        });
    }
  }

  private loadClassOptions(
    unitId: any,
    schoolYearId: any,
    selectedClassId?: any
  ): void {
    const classIdControl = this.enrollmentForm.get('classId');
    const requestId = ++this.classOptionsRequestId;

    if (!selectedClassId) {
      classIdControl?.setValue(null, { emitEvent: false });
    }

    if (!unitId || !schoolYearId) {
      this.items.classItem.options = [];
      this.items.classItem.disabled = true;
      classIdControl?.disable({ emitEvent: false });
      return;
    }

    this.lopService
      .getOptions({ unitId, schoolYearId })
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe({
        next: ({ data }) => {
          if (requestId !== this.classOptionsRequestId) {
            return;
          }

          this.classOptions = (data ?? []).map((item) => ({
            value: item.id,
            label: item.name,
          }));
          this.items.classItem.options = this.classOptions;
          this.items.classItem.disabled = false;
          classIdControl?.enable({ emitEvent: false });

          if (selectedClassId) {
            classIdControl?.setValue(selectedClassId, { emitEvent: false });
          }
        },
        error: () => {
          if (requestId !== this.classOptionsRequestId) {
            return;
          }

          this.classOptions = [];
          this.items.classItem.options = [];
          this.items.classItem.disabled = true;
          classIdControl?.disable({ emitEvent: false });
        },
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
