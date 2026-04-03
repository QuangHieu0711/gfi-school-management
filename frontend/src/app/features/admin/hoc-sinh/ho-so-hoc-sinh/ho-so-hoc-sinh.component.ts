import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

import {
  HOC_SINH_DETAIL_FALLBACK,
  HOC_SINH_GUARDIAN_TYPE,
  HocSinhAddress,
  HocSinhDetailResponse,
  HocSinhGuardian,
} from '@app/model/admin/hoc-sinh.model';

interface InfoItem {
  label: string;
  value: string;
}

interface FlagItem {
  label: string;
  value: boolean;
}

@Component({
  selector: 'app-ho-so-hoc-sinh',
  standalone: true,
  templateUrl: './ho-so-hoc-sinh.component.html',
  styleUrls: ['./ho-so-hoc-sinh.component.scss'],
  imports: [CommonModule],
})
export class HoSoHocSinhComponent {
  @Input() student: HocSinhDetailResponse | null = null;

  get vm(): HocSinhDetailResponse {
    return this.student ?? HOC_SINH_DETAIL_FALLBACK;
  }

  get initials(): string {
    const chunks = (this.vm.fullName ?? '').trim().split(/\s+/).filter(Boolean);
    return (
      chunks
        .slice(0, 2)
        .map((item) => item[0])
        .join('')
        .toUpperCase() || 'HS'
    );
  }

  get summaryItems(): InfoItem[] {
    return [
      { label: 'Mã học sinh', value: this.value(this.vm.studentCode) },
      { label: 'Mã MOET', value: this.value(this.vm.moeCode) },
      { label: 'Trạng thái', value: this.statusLabel(this.vm.studentStatus) },
      {
        label: 'Ngày vào trường',
        value: this.date(this.vm.enrollment?.enrolledAt),
      },
      {
        label: 'Lớp học hiện tại',
        value: this.value(this.vm.enrollment?.className || this.vm.className),
      },
      { label: 'Địa chỉ', value: this.addressLine(this.permanentAddress) },
    ];
  }

  get generalInfo(): InfoItem[] {
    return [
      { label: 'Số định bộ', value: this.value(this.vm.id) },
      { label: 'Mã định danh Bộ GD&ĐT', value: this.value(this.vm.moeCode) },
      { label: 'Hình thức tuyển sinh', value: 'Tuyển sinh' },
    ];
  }

  get addressInfo(): InfoItem[] {
    return [
      {
        label: 'Tỉnh/TP thường trú',
        value: this.value(this.permanentAddress?.provinceName),
      },
      {
        label: 'Quận/huyện thường trú',
        value: this.value(this.permanentAddress?.districtName),
      },
      {
        label: 'Xã/phường thường trú',
        value: this.value(this.permanentAddress?.wardName),
      },
      {
        label: 'Thôn/xóm thường trú',
        value: this.value(this.permanentAddress?.hamletName),
      },
      {
        label: 'Chỗ ở hiện nay',
        value: this.addressLine(this.permanentAddress),
      },
      {
        label: 'Tỉnh/TP nơi sinh',
        value: this.value(this.birthAddress?.provinceName),
      },
      {
        label: 'Xã/phường nơi sinh',
        value: this.value(this.birthAddress?.wardName),
      },
    ];
  }

  get personalInfo(): InfoItem[] {
    return [
      { label: 'Ngày sinh', value: this.date(this.vm.dateOfBirth) },
      { label: 'Giới tính', value: this.genderLabel(this.vm.gender) },
      { label: 'Dân tộc', value: this.value(this.vm.ethnicity) },
      { label: 'Tôn giáo', value: this.value(this.vm.religion) },
      { label: 'Quốc tịch', value: this.value(this.vm.nationality) },
      { label: 'Nhóm máu', value: this.value(this.vm.bloodGroup) },
      { label: 'Điện thoại', value: this.value(this.vm.mobilePhone) },
      { label: 'Email', value: this.value(this.vm.email) },
      { label: 'CCCD', value: this.value(this.vm.identityNumber) },
      { label: 'Ngày cấp', value: this.date(this.vm.identityIssueDate) },
      { label: 'Nơi cấp', value: this.value(this.vm.identityIssuePlace) },
      {
        label: 'Số buổi học/tuần',
        value: this.value(this.vm.enrollment?.sessionsPerWeek),
      },
      {
        label: 'Chế độ học',
        value: this.studyModeLabel(this.vm.enrollment?.studyMode),
      },
      { label: 'BHYT', value: this.value(this.vm.healthInsuranceNumber) },
      {
        label: 'Mã hệ thống khác',
        value: this.value(this.vm.profile?.otherSystemCode),
      },
      { label: 'SSO', value: this.value(this.vm.profile?.ssoCode) },
    ];
  }

  get profileInfo(): InfoItem[] {
    return [
      {
        label: 'Đối tượng chính sách',
        value: this.value(this.vm.profile?.policyObject),
      },
      {
        label: 'Chế độ chính sách',
        value: this.value(this.vm.profile?.policyBenefit),
      },
      {
        label: 'Diện ưu tiên',
        value: this.value(this.vm.profile?.priorityCategory),
      },
      {
        label: 'Diện học sinh',
        value: this.value(this.vm.profile?.studentCategory),
      },
      { label: 'Khu vực', value: this.value(this.vm.profile?.regionCategory) },
      {
        label: 'Loại khuyết tật',
        value: this.value(this.vm.profile?.disabilityType),
      },
      {
        label: 'Ngoại ngữ',
        value: this.value(this.vm.profile?.foreignLanguageProgram),
      },
      {
        label: 'Chứng chỉ ngoại ngữ',
        value: this.value(this.vm.profile?.foreignLanguageCertificate),
      },
      {
        label: 'Chứng chỉ tin học',
        value: this.value(this.vm.profile?.informaticsCertificate),
      },
      {
        label: 'Hướng nghiệp',
        value: this.value(this.vm.profile?.careerOrientation),
      },
      {
        label: 'Nghề phổ thông',
        value: this.value(this.vm.profile?.vocationalOrientation),
      },
      {
        label: 'Ngày vào đội',
        value: this.date(this.vm.profile?.joinedTeamDate),
      },
      {
        label: 'Ngày vào đoàn',
        value: this.date(this.vm.profile?.joinedUnionDate),
      },
      {
        label: 'Ngày vào đảng',
        value: this.date(this.vm.profile?.joinedPartyDate),
      },
    ];
  }

  get supportFlags(): FlagItem[] {
    return [
      {
        label: 'Hỗ trợ chi phí học tập',
        value: !!this.vm.profile?.supportTuitionCost,
      },
      {
        label: 'Thuộc diện tái định cư',
        value: !!this.vm.profile?.resettlementArea,
      },
      { label: 'Hỗ trợ nhà ở', value: !!this.vm.profile?.housingSupport },
      {
        label: 'Cấp tiền hàng tháng',
        value: !!this.vm.profile?.monthlyAllowance,
      },
      { label: 'Hỗ trợ gạo', value: !!this.vm.profile?.riceSupport },
      {
        label: 'Khuyết tật miễn đánh giá',
        value: !!this.vm.profile?.disabilityExemptEval,
      },
      {
        label: 'Học chương trình Bộ',
        value: !!this.vm.profile?.followsMoeProgram,
      },
      {
        label: 'Đã học lớp Mầm 5 tuổi',
        value: !!this.vm.profile?.studiedKindergarten5yo,
      },
      { label: 'Biết bơi', value: !!this.vm.profile?.canSwim },
      {
        label: 'Học ngôn ngữ dân tộc',
        value: !!this.vm.profile?.learnsEthnicLanguage,
      },
      {
        label: 'Cần hỗ trợ tiếng Việt',
        value: !!this.vm.profile?.needsVietnameseSupport,
      },
      {
        label: 'Có tài liệu tăng cường tiếng Việt',
        value: !!this.vm.profile?.hasVietnameseReinforcementMaterial,
      },
      {
        label: 'Có trợ giảng người dân tộc',
        value: !!this.vm.profile?.hasEthnicTeachingAssistant,
      },
      {
        label: 'Phụ huynh có Internet',
        value: !!this.vm.profile?.hasParentInternet,
      },
      {
        label: 'Phụ huynh có smartphone',
        value: !!this.vm.profile?.hasParentSmartphone,
      },
      {
        label: 'Học 2 buổi/ngày',
        value: !!this.vm.enrollment?.isTwoSessionsPerDay,
      },
      { label: 'Nội trú', value: !!this.vm.enrollment?.isBoarding },
      { label: 'Lưu ban', value: !!this.vm.enrollment?.isRepeater },
    ];
  }

  get permanentAddress(): HocSinhAddress | undefined {
    return this.vm.addresses?.find((item) =>
      `${item.addressType}`.toLowerCase().includes('thuong')
    );
  }

  get birthAddress(): HocSinhAddress | undefined {
    return this.vm.addresses?.find((item) =>
      `${item.addressType}`.toLowerCase().includes('sinh')
    );
  }

  get father(): HocSinhGuardian | undefined {
    return this.guardianByType(HOC_SINH_GUARDIAN_TYPE.FATHER, ['CHA']);
  }

  get mother(): HocSinhGuardian | undefined {
    return this.guardianByType(HOC_SINH_GUARDIAN_TYPE.MOTHER, ['ME']);
  }

  get sponsor(): HocSinhGuardian | undefined {
    return this.guardianByType(HOC_SINH_GUARDIAN_TYPE.GUARDIAN);
  }

  guardianItems(guardian?: HocSinhGuardian): InfoItem[] {
    return [
      { label: 'Là dân tộc', value: guardian?.isEthnic ? 'Có' : 'Không' },
      { label: 'Họ và tên', value: this.value(guardian?.fullName) },
      { label: 'Năm sinh', value: this.value(guardian?.birthYear) },
      { label: 'Nghề nghiệp', value: this.value(guardian?.occupation) },
      { label: 'SĐT', value: this.value(guardian?.phone) },
      { label: 'Email', value: this.value(guardian?.email) },
      { label: 'CCCD', value: this.value(guardian?.identityNumber) },
    ];
  }

  statusLabel(status?: number): string {
    switch (status) {
      case 0:
        return 'Đang học';
      case 1:
        return 'Đã chuyển trường';
      case 2:
        return 'Tạm nghỉ';
      case 3:
        return 'Thôi học';
      default:
        return '';
    }
  }

  date(value?: string): string {
    if (!value) return '';
    const raw = value.slice(0, 10);
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  value(value: unknown): string {
    if (value === null || value === undefined || value === '') return '';
    return `${value}`;
  }

  genderLabel(value: unknown): string {
    if (value === 0 || value === '0') return 'Nam';
    if (value === 1 || value === '1' || value === 'Nu') return 'Nữ';
    if (value === 'Nam' || value === 'Nữ') return `${value}`;
    return this.value(value);
  }

  studyModeLabel(value: unknown): string {
    if (value === 0 || value === '0') return 'Học cả ngày';
    if (value === 1 || value === '1') return 'Bán trú';
    if (value === 2 || value === '2') return 'Nội trú';
    return this.value(value);
  }

  private guardianByType(
    type: HOC_SINH_GUARDIAN_TYPE,
    aliases: string[] = []
  ): HocSinhGuardian | undefined {
    const acceptedTypes = [type, ...aliases].map((item) =>
      `${item}`.toUpperCase()
    );

    return this.vm.guardians?.find((item) =>
      acceptedTypes.includes(`${item.guardianType}`.toUpperCase())
    );
  }

  private addressLine(address?: HocSinhAddress): string {
    if (!address) return '';
    return (
      [
        address.detailAddress,
        address.hamletName,
        address.wardName,
        address.districtName,
        address.provinceName,
      ]
        .filter(Boolean)
        .join(', ') || ''
    );
  }
}
