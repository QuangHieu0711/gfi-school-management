import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { FORM_CONTROL_MODULE, MATERIAL_MODULE } from '@modules';
import { DATE_CONTROL, FormType, IOptions, SELECT_CONTROL } from '@model/form-control.model';

import { HocSinhFormRequest } from '@app/model/admin/hoc-sinh.model';
import { DonViService } from '@app/service/admin/don-vi.service';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { LopService } from '@app/service/admin/lop.service';
import { NamHocService } from '@app/service/admin/nam-hoc.service';

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
  unitOptions: IOptions[] = [];
  schoolYearOptions: IOptions[] = [];
  classOptions: IOptions[] = [];
  selectedAvatarName = '';
  readonly genderItem: FormType = SELECT_CONTROL({
    controlName: 'gender',
    placeholder: 'Chon gioi tinh',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly studentStatusItem: FormType = SELECT_CONTROL({
    controlName: 'studentStatus',
    placeholder: 'Chon trang thai hoc sinh',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly admissionTypeItem: FormType = SELECT_CONTROL({
    controlName: 'admissionType',
    placeholder: 'Chon hinh thuc tuyen sinh',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly unitItem: FormType = SELECT_CONTROL({
    controlName: 'unitId',
    placeholder: 'Chon don vi',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly schoolYearItem: FormType = SELECT_CONTROL({
    controlName: 'schoolYearId',
    placeholder: 'Chon nam hoc',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly classItem: FormType = SELECT_CONTROL({
    controlName: 'classId',
    placeholder: 'Chon lop',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly enrollmentStatusItem: FormType = SELECT_CONTROL({
    controlName: 'status',
    placeholder: 'Chon trang thai lop',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly studyModeItem: FormType = SELECT_CONTROL({
    controlName: 'studyMode',
    placeholder: 'Chon che do hoc',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: false,
  });
  readonly dateOfBirthItem: FormType = DATE_CONTROL({
    controlName: 'dateOfBirth',
    placeholder: 'Chon ngay sinh',
    required: false,
    showLabel: false,
  });
  readonly admissionDateItem: FormType = DATE_CONTROL({
    controlName: 'admissionDate',
    placeholder: 'Chon ngay vao truong',
    required: false,
    showLabel: false,
  });
  readonly identityIssueDateItem: FormType = DATE_CONTROL({
    controlName: 'identityIssueDate',
    placeholder: 'Chon ngay cap',
    required: false,
    showLabel: false,
  });
  readonly enrolledAtItem: FormType = DATE_CONTROL({
    controlName: 'enrolledAt',
    placeholder: 'Chon ngay nhap hoc',
    required: false,
    showLabel: false,
  });
  readonly joinedTeamDateItem: FormType = DATE_CONTROL({
    controlName: 'joinedTeamDate',
    placeholder: 'Chon ngay vao doi',
    required: false,
    showLabel: false,
  });
  readonly joinedUnionDateItem: FormType = DATE_CONTROL({
    controlName: 'joinedUnionDate',
    placeholder: 'Chon ngay vao doan',
    required: false,
    showLabel: false,
  });
  readonly joinedPartyDateItem: FormType = DATE_CONTROL({
    controlName: 'joinedPartyDate',
    placeholder: 'Chon ngay vao dang',
    required: false,
    showLabel: false,
  });

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
    private readonly donViService: DonViService,
    private readonly namHocService: NamHocService,
    private readonly lopService: LopService,
    private readonly routerService: Router,
    private readonly locationService: Location
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.form = this.formBuilder.group({
      studentCode: [''],
      fullName: [''],
      firstName: [''],
      moeCode: [''],
      dateOfBirth: [''],
      gender: [null],
      placeOfBirth: [''],
      ethnicity: [''],
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
      studentStatus: [0],
      admissionType: [0],
      unitId: [null],
      enrollment: this.formBuilder.group({
        schoolYearId: [null],
        classId: [null],
        enrolledAt: [''],
        status: [0],
        isRepeater: [false],
        sessionsPerWeek: [0],
        studyMode: [0],
        isBoarding: [false],
        isTwoSessionsPerDay: [false],
      }),
      addresses: this.formBuilder.group({
        permanent: this.formBuilder.group({
          provinceName: [''],
          wardName: [''],
          hamletName: [''],
          detailAddress: [''],
        }),
        temporary: this.formBuilder.group({
          provinceName: [''],
          wardName: [''],
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
      units: this.donViService.getOptions(),
      schoolYears: this.namHocService.getOptions(),
      classes: this.lopService.getOptions(),
    }).subscribe(({ units, schoolYears, classes }) => {
      this.unitOptions = (units.data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));
      this.genderItem.options = this.genderOptions;
      this.studentStatusItem.options = this.studentStatusOptions;
      this.admissionTypeItem.options = this.admissionTypeOptions;
      this.unitItem.options = this.unitOptions;
      this.schoolYearOptions = (schoolYears.data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));
      this.schoolYearItem.options = this.schoolYearOptions;
      this.classOptions = (classes.data ?? []).map((item) => ({
        value: item.id,
        label: item.name,
      }));
      this.classItem.options = this.classOptions;
      this.enrollmentStatusItem.options = this.enrollmentStatusOptions;
      this.studyModeItem.options = this.studyModeOptions;
    });
  }

  goBack(): void {
    this.locationService.back();
  }

  submit(): void {
    const rawValue = this.form.getRawValue();
    const payload = this.buildPayload(rawValue);

    this.hocSinhService.create(payload).subscribe({
      next: ({ data }) => {
        this.toastr.success('Lưu thành công', 'Thành công');
        const createdId = data?.id;
        if (createdId != null) {
          this.routerService.navigate([
            '/',
            NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
            NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
            PATH.CHI_TIET,
            createdId,
          ]);
          return;
        }

        this.routerService.navigate([
          '/',
          NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
          NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
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

  get avatarPreview(): string {
    return `${this.form?.get('avatarUrl')?.value ?? ''}`.trim();
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

  private buildPayload(rawValue: any): HocSinhFormRequest {
    return {
      studentCode: rawValue.studentCode || '',
      fullName: rawValue.fullName || '',
      firstName: rawValue.firstName || '',
      moeCode: rawValue.moeCode || '',
      dateOfBirth: rawValue.dateOfBirth || null,
      gender: this.numberOrZero(rawValue.gender),
      placeOfBirth: rawValue.placeOfBirth || '',
      ethnicity: rawValue.ethnicity || '',
      religion: rawValue.religion || '',
      nationality: rawValue.nationality || '',
      mobilePhone: rawValue.mobilePhone || '',
      email: rawValue.email || '',
      avatarUrl: rawValue.avatarUrl || '',
      identityNumber: rawValue.identityNumber || '',
      identityIssueDate: rawValue.identityIssueDate || null,
      identityIssuePlace: rawValue.identityIssuePlace || '',
      healthInsuranceNumber: rawValue.healthInsuranceNumber || '',
      bloodGroup: rawValue.bloodGroup || '',
      boardingBook: rawValue.boardingBook || '',
      admissionDate: rawValue.admissionDate || null,
      studentStatus: this.numberOrZero(rawValue.studentStatus),
      admissionType: this.numberOrZero(rawValue.admissionType),
      unitId: rawValue.unitId,
      enrollment: {
        schoolYearId: rawValue.enrollment.schoolYearId,
        classId: rawValue.enrollment.classId,
        enrolledAt: rawValue.enrollment.enrolledAt || null,
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
          provinceName: rawValue.addresses.permanent.provinceName || '',
          wardName: rawValue.addresses.permanent.wardName || '',
          hamletName: rawValue.addresses.permanent.hamletName || '',
          detailAddress: rawValue.addresses.permanent.detailAddress || '',
        },
        {
          addressType: 'TAM_TRU',
          provinceName: rawValue.addresses.temporary.provinceName || '',
          wardName: rawValue.addresses.temporary.wardName || '',
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
        joinedTeamDate: rawValue.profile.joinedTeamDate || null,
        joinedUnionDate: rawValue.profile.joinedUnionDate || null,
        joinedPartyDate: rawValue.profile.joinedPartyDate || null,
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
}
