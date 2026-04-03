import {
  DATE_CONTROL,
  SELECT_CONTROL,
  TEXT_CONTROL,
} from '@model/form-control.model';
import { ID_TYPE } from '@model/response.model';
import { TableDataSource, TableRequest } from '@model/table.model';

export enum HOC_SINH_KEY {
  ID = 'id',
  STUDENT_CODE = 'studentCode',
  FULL_NAME = 'fullName',
  FIRST_NAME = 'firstName',
  MOE_CODE = 'moeCode',
  DATE_OF_BIRTH = 'dateOfBirth',
  GENDER = 'gender',
  MOBILE_PHONE = 'mobilePhone',
  EMAIL = 'email',
  IDENTITY_NUMBER = 'identityNumber',
  STUDENT_STATUS = 'studentStatus',
  UNIT_ID = 'unitId',
  CLASS_ID = 'classId',
  CLASS_NAME = 'className',
  GRADE_LEVEL_ID = 'gradeLevelId',
  GRADE_LEVEL_NAME = 'gradeLevelName',
  OTHER_SYSTEM_CODE = 'otherSystemCode',
  FATHER_PHONE = 'fatherPhone',
  MOTHER_PHONE = 'motherPhone',
  PERMANENT_PROVINCE_NAME = 'permanentProvinceName',
  PERMANENT_WARD_NAME = 'permanentWardName',
}

export enum HOC_SINH_GUARDIAN_TYPE {
  FATHER = 'FATHER',
  MOTHER = 'MOTHER',
  GUARDIAN = 'GUARDIAN',
}

export const HOC_SINH_API_ENDPOINT = {
  BASE_PATH: 'students',
  FILTER: 'search',
};

export interface HocSinhFilter {
  fullName?: string;
  firstName?: string;
  studentStatus?: number;
  classId?: ID_TYPE;
  moeCode?: string;
  gradeLevelId?: ID_TYPE;
  dateOfBirth?: string;
  gender?: string;
  studentCode?: string;
  otherSystemCode?: string;
  fatherPhone?: string;
  motherPhone?: string;
  permanentProvinceName?: string;
  permanentWardName?: string;
}

export interface HocSinhFilterRequest extends TableRequest {
  pageNow?: number;
  filter?: HocSinhFilter;
}

export interface HocSinhEnrollment {
  schoolYearId?: ID_TYPE;
  schoolYearName?: string;
  classId?: ID_TYPE;
  className?: string;
  gradeLevelId?: ID_TYPE;
  gradeLevelName?: string;
  enrolledAt?: string;
  status?: number;
  isRepeater?: boolean;
  sessionsPerWeek?: string;
  studyMode?: string;
  isBoarding?: boolean;
  isTwoSessionsPerDay?: boolean;
}

export interface HocSinhAddress {
  addressType?: string;
  provinceName?: string;
  districtName?: string;
  wardName?: string;
  hamletName?: string;
  detailAddress?: string;
}

export interface HocSinhGuardian {
  guardianType?: string;
  fullName?: string;
  birthYear?: number;
  occupation?: string;
  phone?: string;
  email?: string;
  identityNumber?: string;
  isEthnic?: boolean;
}

export interface HocSinhProfile {
  policyObject?: string;
  policyBenefit?: string;
  priorityCategory?: string;
  studentCategory?: string;
  regionCategory?: string;
  disabilityType?: string;
  disabilityExemptEval?: boolean;
  supportTuitionCost?: boolean;
  resettlementArea?: boolean;
  housingSupport?: boolean;
  monthlyAllowance?: boolean;
  riceSupport?: boolean;
  followsMoeProgram?: boolean;
  canSwim?: boolean;
  learnsEthnicLanguage?: boolean;
  studiedKindergarten5yo?: boolean;
  needsVietnameseSupport?: boolean;
  hasVietnameseReinforcementMaterial?: boolean;
  hasEthnicTeachingAssistant?: boolean;
  hasParentInternet?: boolean;
  hasParentSmartphone?: boolean;
  foreignLanguageProgram?: string;
  foreignLanguageCertificate?: string;
  informaticsCertificate?: string;
  careerOrientation?: string;
  vocationalOrientation?: string;
  joinedTeamDate?: string;
  joinedUnionDate?: string;
  joinedPartyDate?: string;
  otherSystemCode?: string;
  ssoCode?: string;
}

export interface HocSinhResponse extends TableDataSource {
  [HOC_SINH_KEY.ID]: ID_TYPE;
  [HOC_SINH_KEY.STUDENT_CODE]?: string;
  [HOC_SINH_KEY.FULL_NAME]: string;
  [HOC_SINH_KEY.FIRST_NAME]?: string;
  [HOC_SINH_KEY.MOE_CODE]?: string;
  [HOC_SINH_KEY.DATE_OF_BIRTH]?: string;
  [HOC_SINH_KEY.GENDER]?: string;
  [HOC_SINH_KEY.MOBILE_PHONE]?: string;
  [HOC_SINH_KEY.EMAIL]?: string;
  [HOC_SINH_KEY.IDENTITY_NUMBER]?: string;
  ethnicity?: string;
  religion?: string;
  nationality?: string;
  bloodGroup?: string;
  identityIssueDate?: string;
  identityIssuePlace?: string;
  healthInsuranceNumber?: string;
  [HOC_SINH_KEY.STUDENT_STATUS]?: number;
  [HOC_SINH_KEY.UNIT_ID]?: ID_TYPE;
  [HOC_SINH_KEY.CLASS_ID]?: ID_TYPE;
  [HOC_SINH_KEY.CLASS_NAME]?: string;
  [HOC_SINH_KEY.GRADE_LEVEL_ID]?: ID_TYPE;
  [HOC_SINH_KEY.GRADE_LEVEL_NAME]?: string;
  [HOC_SINH_KEY.OTHER_SYSTEM_CODE]?: string;
  [HOC_SINH_KEY.FATHER_PHONE]?: string;
  [HOC_SINH_KEY.MOTHER_PHONE]?: string;
  [HOC_SINH_KEY.PERMANENT_PROVINCE_NAME]?: string;
  [HOC_SINH_KEY.PERMANENT_WARD_NAME]?: string;
  enrollment?: HocSinhEnrollment;
  addresses?: HocSinhAddress[];
  guardians?: HocSinhGuardian[];
  profile?: HocSinhProfile;
}

export type HocSinhDetailResponse = HocSinhResponse;

export interface HocSinhFormAddressRequest {
  addressType?: string;
  provinceName?: string;
  wardName?: string;
  hamletName?: string;
  detailAddress?: string;
}

export interface HocSinhFormGuardianRequest {
  guardianType?: string;
  fullName?: string;
  birthYear?: number;
  occupation?: string;
  phone?: string;
  email?: string;
  identityNumber?: string;
  isEthnic?: boolean;
}

export interface HocSinhFormEnrollmentRequest {
  schoolYearId?: ID_TYPE;
  classId?: ID_TYPE;
  enrolledAt?: string;
  status?: number;
  isRepeater?: boolean;
  sessionsPerWeek?: number;
  studyMode?: number;
  isBoarding?: boolean;
  isTwoSessionsPerDay?: boolean;
}

export interface HocSinhFormProfileRequest {
  policyObject?: string;
  policyBenefit?: string;
  priorityCategory?: string;
  studentCategory?: string;
  regionCategory?: string;
  disabilityType?: string;
  disabilityExemptEval?: boolean;
  supportTuitionCost?: boolean;
  resettlementArea?: boolean;
  housingSupport?: boolean;
  monthlyAllowance?: boolean;
  riceSupport?: boolean;
  followsMoeProgram?: boolean;
  canSwim?: boolean;
  learnsEthnicLanguage?: boolean;
  studiedKindergarten5yo?: boolean;
  needsVietnameseSupport?: boolean;
  hasVietnameseReinforcementMaterial?: boolean;
  hasEthnicTeachingAssistant?: boolean;
  hasParentInternet?: boolean;
  hasParentSmartphone?: boolean;
  foreignLanguageProgram?: string;
  foreignLanguageCertificate?: string;
  informaticsCertificate?: string;
  careerOrientation?: string;
  vocationalOrientation?: string;
  joinedTeamDate?: string;
  joinedUnionDate?: string;
  joinedPartyDate?: string;
  otherSystemCode?: string;
  ssoCode?: string;
}

export interface HocSinhFormRequest {
  studentCode?: string;
  fullName?: string;
  firstName?: string;
  moeCode?: string;
  dateOfBirth?: string;
  gender?: number;
  placeOfBirth?: string;
  ethnicity?: string;
  religion?: string;
  nationality?: string;
  mobilePhone?: string;
  email?: string;
  avatarUrl?: string;
  identityNumber?: string;
  identityIssueDate?: string;
  identityIssuePlace?: string;
  healthInsuranceNumber?: string;
  bloodGroup?: string;
  boardingBook?: string;
  admissionDate?: string;
  studentStatus?: number;
  admissionType?: number;
  unitId?: ID_TYPE;
  enrollment?: HocSinhFormEnrollmentRequest;
  addresses?: HocSinhFormAddressRequest[];
  guardians?: HocSinhFormGuardianRequest[];
  profile?: HocSinhFormProfileRequest;
}

export const HOC_SINH_STATUS_OPTIONS = [
  { value: 0, label: 'Đang học' },
  { value: 1, label: 'Đã chuyển trường' },
  { value: 2, label: 'Tạm nghỉ' },
  { value: 3, label: 'Thôi học' },
];

export const HOC_SINH_GENDER_OPTIONS = [
  { value: 'Nam', label: 'Nam' },
  { value: 'Nu', label: 'Nữ' },
];

export const HOC_SINH_FILTER_FORM = [
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.FULL_NAME,
    placeholder: 'Họ và tên',
    required: false,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.FIRST_NAME,
    placeholder: 'Tên',
    required: false,
    maxLength: 255,
  }),
  SELECT_CONTROL({
    controlName: HOC_SINH_KEY.STUDENT_STATUS,
    placeholder: 'Trạng thái',
    required: false,
    clearable: true,
    listOption: HOC_SINH_STATUS_OPTIONS,
  }),
  SELECT_CONTROL({
    controlName: HOC_SINH_KEY.CLASS_ID,
    placeholder: 'Tên lớp',
    required: false,
    clearable: true,
    listOption: [],
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.MOE_CODE,
    placeholder: 'Mã MOET',
    required: false,
    maxLength: 100,
  }),
  SELECT_CONTROL({
    controlName: HOC_SINH_KEY.GRADE_LEVEL_ID,
    placeholder: 'Khối',
    required: false,
    clearable: true,
    listOption: [],
  }),
  DATE_CONTROL({
    controlName: HOC_SINH_KEY.DATE_OF_BIRTH,
    placeholder: 'Ngày sinh',
    required: false,
  }),
  SELECT_CONTROL({
    controlName: HOC_SINH_KEY.GENDER,
    placeholder: 'Giới tính',
    required: false,
    clearable: true,
    listOption: HOC_SINH_GENDER_OPTIONS,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.STUDENT_CODE,
    placeholder: 'Mã học sinh',
    required: false,
    maxLength: 100,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.OTHER_SYSTEM_CODE,
    placeholder: 'Mã hệ thống khác',
    required: false,
    maxLength: 100,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.FATHER_PHONE,
    placeholder: 'Số điện thoại bố',
    required: false,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.MOTHER_PHONE,
    placeholder: 'Số điện thoại mẹ',
    required: false,
    maxLength: 50,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.PERMANENT_PROVINCE_NAME,
    placeholder: 'Tỉnh/TP thường trú',
    required: false,
    maxLength: 255,
  }),
  TEXT_CONTROL({
    controlName: HOC_SINH_KEY.PERMANENT_WARD_NAME,
    placeholder: 'Xã/phường thường trú',
    required: false,
    maxLength: 255,
  }),
];

export const HOC_SINH_DETAIL_FALLBACK: HocSinhDetailResponse = {
  id: '',
  studentCode: '',
  fullName: '',
  firstName: '',
  moeCode: '',
  dateOfBirth: '',
  gender: '',
  studentStatus: undefined,
  classId: undefined,
  className: '',
  gradeLevelId: undefined,
  gradeLevelName: '',
  mobilePhone: '',
  email: '',
  identityNumber: '',
  ethnicity: '',
  religion: '',
  nationality: '',
  bloodGroup: '',
  identityIssueDate: '',
  identityIssuePlace: '',
  healthInsuranceNumber: '',
  enrollment: {
    schoolYearId: undefined,
    schoolYearName: '',
    classId: undefined,
    className: '',
    gradeLevelId: undefined,
    gradeLevelName: '',
    enrolledAt: '',
    status: undefined,
    isRepeater: false,
    sessionsPerWeek: '',
    studyMode: '',
    isBoarding: false,
    isTwoSessionsPerDay: false,
  },
  fatherPhone: '',
  motherPhone: '',
  permanentProvinceName: '',
  permanentWardName: '',
  addresses: [
    {
      addressType: '',
      provinceName: '',
      districtName: '',
      wardName: '',
      hamletName: '',
      detailAddress: '',
    },
    {
      addressType: '',
      provinceName: '',
      districtName: '',
      wardName: '',
      hamletName: '',
      detailAddress: '',
    },
  ],
  guardians: [
    {
      guardianType: HOC_SINH_GUARDIAN_TYPE.FATHER,
      fullName: '',
      birthYear: undefined,
      occupation: '',
      phone: '',
      email: '',
      identityNumber: '',
      isEthnic: false,
    },
    {
      guardianType: HOC_SINH_GUARDIAN_TYPE.MOTHER,
      fullName: '',
      birthYear: undefined,
      occupation: '',
      phone: '',
      email: '',
      identityNumber: '',
      isEthnic: false,
    },
    {
      guardianType: HOC_SINH_GUARDIAN_TYPE.GUARDIAN,
      fullName: '',
      birthYear: undefined,
      occupation: '',
      phone: '',
      email: '',
      identityNumber: '',
      isEthnic: false,
    },
  ],
  profile: {
    policyObject: '',
    policyBenefit: '',
    priorityCategory: '',
    studentCategory: '',
    regionCategory: '',
    disabilityType: '',
    disabilityExemptEval: false,
    supportTuitionCost: false,
    resettlementArea: false,
    housingSupport: false,
    monthlyAllowance: false,
    riceSupport: false,
    followsMoeProgram: false,
    canSwim: false,
    learnsEthnicLanguage: false,
    studiedKindergarten5yo: false,
    needsVietnameseSupport: false,
    hasVietnameseReinforcementMaterial: false,
    hasEthnicTeachingAssistant: false,
    hasParentInternet: false,
    hasParentSmartphone: false,
    foreignLanguageProgram: '',
    foreignLanguageCertificate: '',
    informaticsCertificate: '',
    careerOrientation: '',
    vocationalOrientation: '',
    joinedTeamDate: '',
    joinedUnionDate: '',
    joinedPartyDate: '',
    otherSystemCode: '',
    ssoCode: '',
  },
};
