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
  unitId?: ID_TYPE;
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
  avatarUrl?: string;
  identityIssueDate?: string;
  identityIssuePlace?: string;
  healthInsuranceNumber?: string;
  [HOC_SINH_KEY.STUDENT_STATUS]?: number;
  [HOC_SINH_KEY.UNIT_ID]?: ID_TYPE;
  unitName?: string;
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
    controlName: HOC_SINH_KEY.UNIT_ID,
    placeholder: 'Đơn vị',
    required: false,
    clearable: true,
    listOption: [],
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
    hint: 'Mã học sinh hệ thống tự sinh theo quy tắc: HS_[Năm học]_[Số thứ tự]',
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

export const GENDER_OPTIONS = [
  { value: 0, label: 'Nam' },
  { value: 1, label: 'Nữ' },
];

export const STUDENT_STATUS_OPTIONS = [
  { value: 0, label: 'Đang học' },
  { value: 1, label: 'Đã chuyển trường' },
  { value: 2, label: 'Tạm nghỉ' },
  { value: 3, label: 'Thôi học' },
];

export const ADMISSION_TYPE_OPTIONS = [
  { value: 0, label: 'Xét tuyển' },
  { value: 1, label: 'Thi tuyển' },
];

export const STUDY_MODE_OPTIONS = [
  { value: 0, label: 'Học cả ngày' },
  { value: 1, label: 'Bán trú' },
  { value: 2, label: 'Nội trú' },
];

export const ENROLLMENT_STATUS_OPTIONS = [
  { value: 0, label: 'Đang học' },
  { value: 1, label: 'Đã chuyển lớp' },
  { value: 2, label: 'Tạm dừng' },
];

export const HOC_SINH_FORM_ITEM = {
  genderItem: SELECT_CONTROL({
    label: 'Giới tính',
    controlName: 'gender',
    placeholder: 'Giới tính',
    required: false,
    clearable: true,
    listOption: GENDER_OPTIONS,
    showLabel: true,
  }),
  studentStatusItem: SELECT_CONTROL({
    label: 'Trạng thái học sinh',
    controlName: 'studentStatus',
    placeholder: 'Trạng thái học sinh',
    required: false,
    clearable: true,
    listOption: STUDENT_STATUS_OPTIONS,
    showLabel: true,
  }),
  admissionTypeItem: SELECT_CONTROL({
    label: 'Hình thức tuyển sinh',
    controlName: 'admissionType',
    placeholder: 'Hình thức tuyển sinh',
    required: false,
    clearable: true,
    listOption: ADMISSION_TYPE_OPTIONS,
    showLabel: true,
  }),
  unitItem: SELECT_CONTROL({
    label: 'Đơn vị',
    controlName: 'unitId',
    placeholder: 'Đơn vị',
    required: true,
    clearable: true,
    listOption: [],
    showLabel: true,
  }),
  schoolYearItem: SELECT_CONTROL({
    label: 'Năm học',
    controlName: 'schoolYearId',
    placeholder: 'Năm học',
    required: true,
    clearable: true,
    listOption: [],
    showLabel: true,
  }),
  classItem: SELECT_CONTROL({
    label: 'Lớp',
    controlName: 'classId',
    placeholder: 'Chọn lớp',
    required: true,
    clearable: true,
    listOption: [],
    showLabel: true,
  }),
  enrollmentStatusItem: SELECT_CONTROL({
    label: 'Trạng thái lớp',
    controlName: 'status',
    placeholder: 'Chọn trạng thái lớp',
    required: false,
    clearable: true,
    listOption: ENROLLMENT_STATUS_OPTIONS,
    showLabel: true,
  }),
  studyModeItem: SELECT_CONTROL({
    label: 'Chế độ học',
    controlName: 'studyMode',
    placeholder: 'Chọn chế độ học',
    required: false,
    clearable: true,
    listOption: STUDY_MODE_OPTIONS,
    showLabel: true,
  }),
  dateOfBirthItem: DATE_CONTROL({
    label: 'Ngày sinh',
    controlName: 'dateOfBirth',
    placeholder: 'Chọn ngày sinh',
    required: true,
    showLabel: true,
  }),
  admissionDateItem: DATE_CONTROL({
    label: 'Ngày vào trường',
    controlName: 'admissionDate',
    placeholder: 'Chọn ngày vào trường',
    required: false,
    showLabel: true,
  }),
  identityIssueDateItem: DATE_CONTROL({
    label: 'Ngày cấp',
    controlName: 'identityIssueDate',
    placeholder: 'Chọn ngày cấp',
    required: false,
    showLabel: true,
  }),
  enrolledAtItem: DATE_CONTROL({
    label: 'Ngày nhập học',
    controlName: 'enrolledAt',
    placeholder: 'Chọn ngày nhập học',
    required: false,
    showLabel: true,
  }),
  joinedTeamDateItem: DATE_CONTROL({
    label: 'Ngày vào đội',
    controlName: 'joinedTeamDate',
    placeholder: 'Chọn ngày vào đội',
    required: false,
    showLabel: true,
  }),
  joinedUnionDateItem: DATE_CONTROL({
    label: 'Ngày vào đoàn',
    controlName: 'joinedUnionDate',
    placeholder: 'Chọn ngày vào đoàn',
    required: false,
    showLabel: true,
  }),
  joinedPartyDateItem: DATE_CONTROL({
    label: 'Ngày vào đảng',
    controlName: 'joinedPartyDate',
    placeholder: 'Chọn ngày vào đảng',
    required: false,
    showLabel: true,
  }),
  permanentProvinceItem: SELECT_CONTROL({
    label: 'Tỉnh/TP',
    controlName: 'provinceName',
    placeholder: 'Chọn tỉnh/TP',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: true,
  }),
  permanentWardItem: SELECT_CONTROL({
    label: 'Xã/phường',
    controlName: 'wardName',
    placeholder: 'Chọn xã/phường',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: true,
    disabled: true,
  }),
  temporaryProvinceItem: SELECT_CONTROL({
    label: 'Tỉnh/TP',
    controlName: 'provinceName',
    placeholder: 'Chọn tỉnh/TP',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: true,
  }),
  temporaryWardItem: SELECT_CONTROL({
    label: 'Xã/phường',
    controlName: 'wardName',
    placeholder: 'Chọn xã/phường',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: true,
    disabled: true,
  }),
  ethnicityItem: SELECT_CONTROL({
    label: 'Dân tộc',
    controlName: 'ethnicity',
    placeholder: 'Dân tộc',
    required: false,
    clearable: true,
    listOption: [],
    showLabel: true,
  }),
  placeOfBirthItem: TEXT_CONTROL({
    controlName: 'placeOfBirth',
    label: 'Nơi sinh',
    placeholder: 'Nơi sinh',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  religionItem: TEXT_CONTROL({
    controlName: 'religion',
    label: 'Tôn giáo',
    placeholder: 'Tôn giáo',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  nationalityItem: TEXT_CONTROL({
    controlName: 'nationality',
    label: 'Quốc tịch',
    placeholder: 'Quốc tịch',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  mobilePhoneItem: TEXT_CONTROL({
    controlName: 'mobilePhone',
    label: 'Điện thoại',
    placeholder: 'Số điện thoại',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  emailItem: TEXT_CONTROL({
    controlName: 'email',
    label: 'Email',
    placeholder: 'Email',
    type: 'email',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  identityNumberItem: TEXT_CONTROL({
    controlName: 'identityNumber',
    label: 'CCCD',
    placeholder: 'CCCD',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  identityIssuePlaceItem: TEXT_CONTROL({
    controlName: 'identityIssuePlace',
    label: 'Nơi cấp',
    placeholder: 'Nơi cấp',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  healthInsuranceNumberItem: TEXT_CONTROL({
    controlName: 'healthInsuranceNumber',
    label: 'BHYT',
    placeholder: 'Số BHYT',
    required: false,
    maxLength: 100,
    showLabel: true,
  }),
  bloodGroupItem: TEXT_CONTROL({
    controlName: 'bloodGroup',
    label: 'Nhóm máu',
    placeholder: 'Nhóm máu',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  boardingBookItem: TEXT_CONTROL({
    controlName: 'boardingBook',
    label: 'Số hộ khẩu',
    placeholder: 'Số hộ khẩu',
    required: false,
    maxLength: 100,
    showLabel: true,
  }),
  studentCodeItem: TEXT_CONTROL({
    controlName: 'studentCode',
    label: 'Mã học sinh',
    placeholder: 'Mã học sinh',
    required: true,
    maxLength: 100,
    showLabel: true,
  }),
  fullNameItem: TEXT_CONTROL({
    controlName: 'fullName',
    label: 'Họ và tên',
    placeholder: 'Họ và tên',
    required: true,
    maxLength: 255,
    showLabel: true,
  }),
  firstNameItem: TEXT_CONTROL({
    controlName: 'firstName',
    label: 'Tên',
    placeholder: 'Tên',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  moeCodeItem: TEXT_CONTROL({
    controlName: 'moeCode',
    label: 'Mã MOET',
    placeholder: 'Mã MOET',
    required: false,
    maxLength: 100,
    showLabel: true,
  }),
  fatherFullNameItem: TEXT_CONTROL({
    controlName: 'fullName',
    label: 'Họ và tên',
    placeholder: 'Họ và tên cha',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  fatherBirthYearItem: TEXT_CONTROL({
    controlName: 'birthYear',
    label: 'Năm sinh',
    placeholder: 'Năm sinh',
    type: 'number',
    required: false,
    maxLength: 4,
    showLabel: true,
  }),
  fatherOccupationItem: TEXT_CONTROL({
    controlName: 'occupation',
    label: 'Nghề nghiệp',
    placeholder: 'Nghề nghiệp',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  fatherPhoneItem: TEXT_CONTROL({
    controlName: 'phone',
    label: 'SDT',
    placeholder: 'Số điện thoại',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  fatherEmailItem: TEXT_CONTROL({
    controlName: 'email',
    label: 'Email',
    placeholder: 'Email',
    type: 'email',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  fatherIdentityNumberItem: TEXT_CONTROL({
    controlName: 'identityNumber',
    label: 'CCCD',
    placeholder: 'CCCD',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  motherFullNameItem: TEXT_CONTROL({
    controlName: 'fullName',
    label: 'Họ và tên',
    placeholder: 'Họ và tên mẹ',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  motherBirthYearItem: TEXT_CONTROL({
    controlName: 'birthYear',
    label: 'Năm sinh',
    placeholder: 'Năm sinh',
    type: 'number',
    required: false,
    maxLength: 4,
    showLabel: true,
  }),
  motherOccupationItem: TEXT_CONTROL({
    controlName: 'occupation',
    label: 'Nghề nghiệp',
    placeholder: 'Nghề nghiệp',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  motherPhoneItem: TEXT_CONTROL({
    controlName: 'phone',
    label: 'SDT',
    placeholder: 'Số điện thoại',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  motherEmailItem: TEXT_CONTROL({
    controlName: 'email',
    label: 'Email',
    placeholder: 'Email',
    type: 'email',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  motherIdentityNumberItem: TEXT_CONTROL({
    controlName: 'identityNumber',
    label: 'CCCD',
    placeholder: 'CCCD',
    required: false,
    maxLength: 50,
    showLabel: true,
  }),
  sessionsPerWeekItem: TEXT_CONTROL({
    controlName: 'sessionsPerWeek',
    label: 'Số buổi học/tuần',
    placeholder: 'Số buổi học/tuần',
    type: 'number',
    required: false,
    maxLength: 3,
    showLabel: true,
  }),
  permanentHamletNameItem: TEXT_CONTROL({
    controlName: 'hamletName',
    label: 'Thôn/Xóm',
    placeholder: 'Thôn/xóm',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  permanentDetailAddressItem: TEXT_CONTROL({
    controlName: 'detailAddress',
    label: 'Địa chỉ chi tiết',
    placeholder: 'Địa chỉ chi tiết',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  temporaryHamletNameItem: TEXT_CONTROL({
    controlName: 'hamletName',
    label: 'Thôn/Xóm',
    placeholder: 'Thôn/xóm',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  temporaryDetailAddressItem: TEXT_CONTROL({
    controlName: 'detailAddress',
    label: 'Địa chỉ chi tiết',
    placeholder: 'Địa chỉ chi tiết',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  policyObjectItem: TEXT_CONTROL({
    controlName: 'policyObject',
    label: 'Đối tượng chính sách',
    placeholder: 'Đối tượng chính sách',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  policyBenefitItem: TEXT_CONTROL({
    controlName: 'policyBenefit',
    label: 'Chế độ chính sách',
    placeholder: 'Chế độ chính sách',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  priorityCategoryItem: TEXT_CONTROL({
    controlName: 'priorityCategory',
    label: 'Điểm ưu tiên',
    placeholder: 'Điểm ưu tiên',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  studentCategoryItem: TEXT_CONTROL({
    controlName: 'studentCategory',
    label: 'Điểm học sinh',
    placeholder: 'Điểm học sinh',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  regionCategoryItem: TEXT_CONTROL({
    controlName: 'regionCategory',
    label: 'Khu vực',
    placeholder: 'Khu vực',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  disabilityTypeItem: TEXT_CONTROL({
    controlName: 'disabilityType',
    label: 'Loại khuyết tật',
    placeholder: 'Loại khuyết tật',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  foreignLanguageProgramItem: TEXT_CONTROL({
    controlName: 'foreignLanguageProgram',
    label: 'Chương trình ngoại ngữ',
    placeholder: 'Chương trình ngoại ngữ',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  foreignLanguageCertificateItem: TEXT_CONTROL({
    controlName: 'foreignLanguageCertificate',
    label: 'Chứng chỉ ngoại ngữ',
    placeholder: 'Chứng chỉ ngoại ngữ',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  informaticsCertificateItem: TEXT_CONTROL({
    controlName: 'informaticsCertificate',
    label: 'Chứng chỉ tin học',
    placeholder: 'Chứng chỉ tin học',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  careerOrientationItem: TEXT_CONTROL({
    controlName: 'careerOrientation',
    label: 'Hướng nghiệp',
    placeholder: 'Hướng nghiệp',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  vocationalOrientationItem: TEXT_CONTROL({
    controlName: 'vocationalOrientation',
    label: 'Nghề nghiệp',
    placeholder: 'Nghề nghiệp',
    required: false,
    maxLength: 255,
    showLabel: true,
  }),
  otherSystemCodeItem: TEXT_CONTROL({
    controlName: 'otherSystemCode',
    label: 'Mã hệ thống khác',
    placeholder: 'Mã hệ thống khác',
    required: false,
    maxLength: 100,
    showLabel: true,
  }),
  ssoCodeItem: TEXT_CONTROL({
    controlName: 'ssoCode',
    label: 'SSO',
    placeholder: 'Mã SSO',
    required: false,
    maxLength: 100,
    showLabel: true,
  }),
  avatarUrlItem: TEXT_CONTROL({
    controlName: 'avatarUrl',
    label: 'Ảnh đại diện',
    placeholder: 'Ảnh đại diện',
    required: false,
    showLabel: false,
  }),
};

export const HOC_SINH_BASIC_SUB_FORM = [
  HOC_SINH_FORM_ITEM.studentCodeItem,
  HOC_SINH_FORM_ITEM.fullNameItem,
  HOC_SINH_FORM_ITEM.firstNameItem,
  HOC_SINH_FORM_ITEM.moeCodeItem,
  HOC_SINH_FORM_ITEM.dateOfBirthItem,
  HOC_SINH_FORM_ITEM.genderItem,
  HOC_SINH_FORM_ITEM.admissionDateItem,
  HOC_SINH_FORM_ITEM.studentStatusItem,
  HOC_SINH_FORM_ITEM.admissionTypeItem,
  HOC_SINH_FORM_ITEM.unitItem,
  HOC_SINH_FORM_ITEM.ethnicityItem,
  HOC_SINH_FORM_ITEM.placeOfBirthItem,
  HOC_SINH_FORM_ITEM.religionItem,
  HOC_SINH_FORM_ITEM.nationalityItem,
  HOC_SINH_FORM_ITEM.mobilePhoneItem,
  HOC_SINH_FORM_ITEM.emailItem,
  HOC_SINH_FORM_ITEM.identityNumberItem,
  HOC_SINH_FORM_ITEM.identityIssueDateItem,
  HOC_SINH_FORM_ITEM.identityIssuePlaceItem,
  HOC_SINH_FORM_ITEM.healthInsuranceNumberItem,
  HOC_SINH_FORM_ITEM.bloodGroupItem,
  HOC_SINH_FORM_ITEM.boardingBookItem,
];

export const HOC_SINH_ENROLLMENT_SUB_FORM = [
  HOC_SINH_FORM_ITEM.schoolYearItem,
  HOC_SINH_FORM_ITEM.classItem,
  HOC_SINH_FORM_ITEM.enrolledAtItem,
  HOC_SINH_FORM_ITEM.enrollmentStatusItem,
  HOC_SINH_FORM_ITEM.studyModeItem,
  HOC_SINH_FORM_ITEM.sessionsPerWeekItem,
];

export const HOC_SINH_ADDRESS_SUB_FORM = [
  HOC_SINH_FORM_ITEM.permanentProvinceItem,
  HOC_SINH_FORM_ITEM.permanentWardItem,
  HOC_SINH_FORM_ITEM.permanentHamletNameItem,
  HOC_SINH_FORM_ITEM.permanentDetailAddressItem,
];

export const HOC_SINH_GUARDIAN_SUB_FORM = [
  HOC_SINH_FORM_ITEM.fatherFullNameItem,
  HOC_SINH_FORM_ITEM.fatherBirthYearItem,
  HOC_SINH_FORM_ITEM.fatherOccupationItem,
  HOC_SINH_FORM_ITEM.fatherPhoneItem,
  HOC_SINH_FORM_ITEM.fatherEmailItem,
  HOC_SINH_FORM_ITEM.fatherIdentityNumberItem,
];

export const HOC_SINH_PROFILE_SUB_FORM = [
  HOC_SINH_FORM_ITEM.policyObjectItem,
  HOC_SINH_FORM_ITEM.policyBenefitItem,
  HOC_SINH_FORM_ITEM.priorityCategoryItem,
  HOC_SINH_FORM_ITEM.studentCategoryItem,
  HOC_SINH_FORM_ITEM.regionCategoryItem,
  HOC_SINH_FORM_ITEM.disabilityTypeItem,
  HOC_SINH_FORM_ITEM.foreignLanguageProgramItem,
  HOC_SINH_FORM_ITEM.foreignLanguageCertificateItem,
  HOC_SINH_FORM_ITEM.informaticsCertificateItem,
  HOC_SINH_FORM_ITEM.careerOrientationItem,
  HOC_SINH_FORM_ITEM.vocationalOrientationItem,
  HOC_SINH_FORM_ITEM.joinedTeamDateItem,
  HOC_SINH_FORM_ITEM.joinedUnionDateItem,
  HOC_SINH_FORM_ITEM.joinedPartyDateItem,
  HOC_SINH_FORM_ITEM.otherSystemCodeItem,
  HOC_SINH_FORM_ITEM.ssoCodeItem,
];
