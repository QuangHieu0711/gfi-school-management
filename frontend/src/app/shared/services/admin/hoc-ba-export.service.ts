/* eslint-disable @typescript-eslint/no-explicit-any */
import { Injectable } from '@angular/core';
import { HocSinhResponse, HocSinhGuardian, HocSinhAddress } from '@app/model/admin/hoc-sinh.model';

@Injectable({ providedIn: 'root' })
export class HocBaExportService {
  // ─── PUBLIC API ────────────────────────────────────────────────────────────

  exportToPdf(students: HocSinhResponse[]): void {
    const html = this.buildHtmlDocument(students, 'pdf');
    const win = window.open('', '_blank', 'width=1200,height=900');
    if (!win) {
      throw new Error('Popup bị chặn. Vui lòng cho phép popup để xuất PDF.');
    }
    win.document.write(html);
    win.document.close();
    // Give browser time to render before printing
    win.onload = () => {
      setTimeout(() => {
        win.focus();
        win.print();
      }, 600);
    };
  }

  exportToExcel(students: HocSinhResponse[]): void {
    const html = this.buildHtmlDocument(students, 'excel');
    const blob = new Blob([html], {
      type: 'application/vnd.ms-excel;charset=utf-8',
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `Hoc-Ba-${this.formatDateFilename()}.xls`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  }

  // ─── HTML BUILDER ───────────────────────────────────────────────────────────

  private buildHtmlDocument(
    students: HocSinhResponse[],
    mode: 'pdf' | 'excel'
  ): string {
    const pages = students.map((s) => this.buildStudentPages(s)).join('');
    return `<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>Học Bạ - Bộ Giáo Dục và Đào Tạo</title>
<style>
  ${this.getGlobalStyles(mode)}
</style>
</head>
<body>
${pages}
</body>
</html>`;
  }

  private getGlobalStyles(mode: 'pdf' | 'excel'): string {
    const printCss =
      mode === 'pdf'
        ? `
      @media print {
        body { margin: 0; }
        .page-break { page-break-before: always; }
        .no-print { display: none !important; }
        @page { size: A4; margin: 15mm 15mm 15mm 20mm; }
      }`
        : '';

    return `
      * { box-sizing: border-box; margin: 0; padding: 0; }
      body {
        font-family: 'Times New Roman', Times, serif;
        font-size: 12pt;
        color: #000;
        background: #f0f0f0;
      }
      .page-wrapper {
        background: #fff;
        width: 210mm;
        min-height: 297mm;
        margin: 20px auto;
        padding: 20mm 15mm 15mm 20mm;
        box-shadow: 0 2px 12px rgba(0,0,0,0.18);
        position: relative;
      }
      .page-break { page-break-before: always; }

      /* ─── TRANG BÌA ─── */
      .cover-page {
        display: flex;
        flex-direction: column;
        height: 257mm;
      }
      .cover-split {
        display: flex;
        gap: 0;
        flex: 1;
      }
      .cover-left {
        flex: 0 0 90mm;
        border: 1.5px solid #000;
        padding: 10mm 8mm;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
      }
      .cover-right {
        flex: 1;
        padding: 0 0 0 10mm;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
      }
      .ministry-header {
        text-align: center;
        font-size: 12pt;
        font-weight: bold;
        text-transform: uppercase;
        line-height: 1.8;
        margin-bottom: 20mm;
      }
      .title-center {
        text-align: center;
        margin: auto 0;
      }
      .hoc-ba-title {
        font-size: 18pt;
        font-weight: bold;
        text-transform: uppercase;
        text-align: center;
        display: block;
        margin-bottom: 4mm;
      }
      .hoc-ba-subtitle {
        font-size: 14pt;
        font-weight: bold;
        text-transform: uppercase;
        text-align: center;
        display: block;
      }
      .cover-fields {
        display: flex;
        flex-direction: column;
        gap: 6mm;
        margin-top: 6mm;
      }
      .cover-field-row {
        display: flex;
        flex-direction: column;
        gap: 1.5mm;
      }
      .cover-field-label {
        font-size: 11pt;
        font-weight: bold;
      }
      .cover-field-line {
        border-bottom: 1px dotted #000;
        min-height: 6mm;
        display: block;
      }
      .cover-right-title {
        text-align: center;
        font-size: 16pt;
        font-weight: bold;
        text-transform: uppercase;
        margin-bottom: 10mm;
      }
      .right-info-grid {
        display: flex;
        flex-direction: column;
        gap: 5mm;
        flex: 1;
      }
      .right-info-row {
        display: flex;
        flex-direction: column;
        gap: 1.5mm;
      }
      .right-info-label {
        font-size: 11pt;
        font-weight: normal;
      }
      .right-info-line {
        border-bottom: 1px dotted #000;
        min-height: 6mm;
        display: block;
      }
      .right-info-line-value {
        font-size: 11pt;
        padding-bottom: 1mm;
      }
      .right-inline {
        display: flex;
        gap: 5mm;
      }
      .right-inline-item {
        display: flex;
        flex-direction: column;
        gap: 1.5mm;
      }
      .right-inline-label {
        font-size: 11pt;
        font-weight: normal;
        white-space: nowrap;
      }
      .right-inline-line {
        border-bottom: 1px dotted #000;
        min-height: 6mm;
        min-width: 28mm;
        display: block;
      }
      .signature-block {
        text-align: right;
        margin-top: 8mm;
      }
      .signature-date {
        font-size: 11pt;
        font-style: italic;
        margin-bottom: 2mm;
      }
      .signature-title {
        font-size: 12pt;
        font-weight: bold;
        text-transform: uppercase;
      }
      .signature-note {
        font-size: 10pt;
        font-style: italic;
      }

      /* ─── TRANG QUÁ TRÌNH HỌC TẬP ─── */
      .content-page {
        display: flex;
        gap: 12mm;
      }
      .content-left {
        flex: 0 0 80mm;
        display: flex;
        flex-direction: column;
        gap: 8mm;
      }
      .content-right {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 6mm;
      }
      .section-title {
        font-size: 12pt;
        font-weight: bold;
        text-transform: uppercase;
        text-align: center;
        margin-bottom: 4mm;
      }

      /* ─── BẢNG ─── */
      table {
        width: 100%;
        border-collapse: collapse;
        font-size: 10.5pt;
      }
      th, td {
        border: 1px solid #000;
        padding: 2mm 2mm;
        text-align: center;
        vertical-align: middle;
        font-family: 'Times New Roman', Times, serif;
      }
      th {
        font-weight: bold;
        background: #fff;
      }
      td.left {
        text-align: left;
      }
      .dot-lines {
        display: flex;
        flex-direction: column;
        gap: 2mm;
        padding: 1mm 2mm;
      }
      .dot-line {
        border-bottom: 1px dotted #555;
        min-height: 5mm;
        display: block;
        width: 100%;
      }

      /* ─── INFO HEADER ─── */
      .info-header {
        display: flex;
        flex-direction: column;
        gap: 3mm;
        margin-bottom: 5mm;
      }
      .info-row {
        display: flex;
        gap: 4mm;
        font-size: 11pt;
      }
      .info-label {
        font-weight: normal;
        white-space: nowrap;
      }
      .info-value-line {
        flex: 1;
        border-bottom: 1px dotted #000;
        min-height: 5mm;
        display: inline-block;
        font-size: 11pt;
        padding-bottom: 0.5mm;
      }

      /* ─── NHẬN XÉT CUỐI ─── */
      .final-comment {
        margin-top: 6mm;
      }
      .final-label {
        font-size: 11pt;
        font-weight: bold;
        margin-bottom: 2mm;
      }
      .comment-lines {
        display: flex;
        flex-direction: column;
        gap: 5mm;
      }
      .comment-line {
        border-bottom: 1px dotted #000;
        min-height: 6mm;
        display: block;
      }
      .dual-signature {
        display: flex;
        justify-content: space-between;
        margin-top: 8mm;
      }
      .sig-block {
        display: flex;
        flex-direction: column;
        gap: 2mm;
        text-align: center;
      }
      .sig-title {
        font-size: 11pt;
        font-weight: bold;
      }
      .sig-note {
        font-size: 10pt;
        font-style: italic;
      }
      .date-right {
        text-align: right;
        font-size: 10.5pt;
        font-style: italic;
        margin-bottom: 3mm;
      }
      ${printCss}
    `;
  }

  // ─── BUILD PAGES FOR ONE STUDENT ────────────────────────────────────────────

  private buildStudentPages(student: HocSinhResponse, isFirst = true): string {
    const coverPage = this.buildCoverPage(student);
    const contentPage = this.buildContentPage(student);
    const breakClass = isFirst ? '' : 'page-break';
    return `
      <div class="page-wrapper ${breakClass}">${coverPage}</div>
      <div class="page-wrapper page-break">${contentPage}</div>
    `;
  }

  // ─── TRANG BÌA ──────────────────────────────────────────────────────────────

  private buildCoverPage(student: HocSinhResponse): string {
    const fullName = student.fullName ?? '';
    const className = student.className ?? student.enrollment?.className ?? '';
    const unitName = student.unitName ?? '';
    const province = this.getProvinceName(student);
    const district = this.getDistrictName(student);
    const dob = this.formatDate(student.dateOfBirth);
    const ethnicity = student.ethnicity ?? '';
    const nationality = student.nationality ?? '';
    const placeOfBirth = this.getField(student, 'placeOfBirth');
    const hometown = this.getField(student, 'homeTown') || province;
    const currentAddress = this.getCurrentAddress(student);
    const fatherName = this.getGuardianField(student, 'FATHER', 'fullName');
    const motherName = this.getGuardianField(student, 'MOTHER', 'fullName');
    const guardian = this.getGuardianField(student, 'GUARDIAN', 'fullName');

    return `
      <div class="cover-page">
        <div class="cover-split">
          <!-- Trái: Trang bìa bên trái -->
          <div class="cover-left">
            <div class="ministry-header">
              BỘ GIÁO DỤC VÀ ĐÀO TẠO
            </div>
            <div class="title-center">
              <span class="hoc-ba-title">Học Bạ</span>
              <span class="hoc-ba-subtitle">Tiểu học</span>
            </div>
            <div class="cover-fields">
              <div class="cover-field-row">
                <span class="cover-field-label">Họ và tên học sinh:</span>
                <span class="cover-field-line">${fullName}</span>
              </div>
              <div class="cover-field-row">
                <span class="cover-field-label">Trường:</span>
                <span class="cover-field-line">${unitName}</span>
              </div>
              <div class="cover-field-row">
                <span class="cover-field-label">Xã (Phường, Thị trấn):</span>
                <span class="cover-field-line">${district}</span>
              </div>
              <div class="cover-field-row">
                <span class="cover-field-label">Huyện (Thành phố, Quận, Thị xã):</span>
                <span class="cover-field-line"></span>
              </div>
              <div class="cover-field-row">
                <span class="cover-field-label">Tỉnh (Thành phố):</span>
                <span class="cover-field-line">${province}</span>
              </div>
            </div>
          </div>

          <!-- Phải: Trang bìa bên phải -->
          <div class="cover-right">
            <div class="cover-right-title">HỌC BẠ</div>
            <div class="right-info-grid">
              <div>
                <div class="right-inline" style="margin-bottom: 3mm;">
                  <div class="right-inline-item" style="flex:1;">
                    <span class="right-inline-label">Họ và tên học sinh:</span>
                    <span class="right-inline-line">${fullName}</span>
                  </div>
                  <div class="right-inline-item" style="flex:0 0 auto;">
                    <span class="right-inline-label">Giới tính:</span>
                    <span class="right-inline-line">${this.formatGender(student)}</span>
                  </div>
                </div>
              </div>

              <div>
                <div class="right-inline" style="margin-bottom: 3mm;">
                  <div class="right-inline-item" style="flex:1;">
                    <span class="right-inline-label">Ngày, tháng, năm sinh:</span>
                    <span class="right-inline-line">${dob}</span>
                  </div>
                  <div class="right-inline-item">
                    <span class="right-inline-label">Dân tộc:</span>
                    <span class="right-inline-line">${ethnicity}</span>
                  </div>
                  <div class="right-inline-item">
                    <span class="right-inline-label">Quốc tịch:</span>
                    <span class="right-inline-line">${nationality}</span>
                  </div>
                </div>
              </div>

              <div class="right-info-row">
                <span class="right-info-label">Nơi sinh:</span>
                <span class="right-info-line">${placeOfBirth}</span>
              </div>
              <div class="right-info-row">
                <span class="right-info-label">Quê quán:</span>
                <span class="right-info-line">${hometown}</span>
              </div>
              <div class="right-info-row">
                <span class="right-info-label">Nơi ở hiện nay:</span>
                <span class="right-info-line">${currentAddress}</span>
              </div>
              <div class="right-info-row">
                <span class="right-info-label">Họ và tên cha:</span>
                <span class="right-info-line">${fatherName}</span>
              </div>
              <div class="right-info-row">
                <span class="right-info-label">Họ và tên mẹ:</span>
                <span class="right-info-line">${motherName}</span>
              </div>
              <div class="right-info-row">
                <span class="right-info-label">Người giám hộ (nếu có):</span>
                <span class="right-info-line">${guardian}</span>
              </div>
            </div>

            <div class="signature-block">
              <div class="signature-date">....... ngày ....... tháng ....... năm 20......</div>
              <div class="signature-title">HIỆU TRƯỞNG</div>
              <div class="signature-note">(Ký, ghi rõ họ tên và đóng dấu)</div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  // ─── TRANG KẾT QUẢ HỌC TẬP ─────────────────────────────────────────────────

  private buildContentPage(student: HocSinhResponse): string {
    const fullName = student.fullName ?? '';
    const className = student.className ?? student.enrollment?.className ?? '';
    const schoolYear = student.enrollment?.schoolYearName ?? '';

    return `
      <div class="content-page">
        <!-- Bên trái: Quá trình học tập -->
        <div class="content-left">
          <div class="section-title">Quá trình học tập</div>
          <table>
            <thead>
              <tr>
                <th style="width:20%">Năm học</th>
                <th style="width:10%">Lớp</th>
                <th style="width:30%">Tên trường</th>
                <th style="width:18%">Số đăng bộ</th>
                <th style="width:22%">Ngày nhập học/ chuyển đến</th>
              </tr>
            </thead>
            <tbody>
              ${this.buildLearningHistoryRows(student)}
            </tbody>
          </table>
        </div>

        <!-- Bên phải: Kết quả học tập -->
        <div class="content-right">
          <!-- Header thông tin học sinh -->
          <div class="info-header">
            <div class="info-row">
              <span class="info-label">Họ và tên học sinh:</span>
              <span class="info-value-line">${fullName}</span>
              <span class="info-label" style="margin-left:4mm;">Lớp:</span>
              <span class="info-value-line" style="max-width:20mm;">${className}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Chiều cao:</span>
              <span class="info-value-line" style="max-width:35mm;"></span>
              <span class="info-label" style="margin-left:4mm;">Cân nặng:</span>
              <span class="info-value-line"></span>
            </div>
            <div class="info-row">
              <span class="info-label">Số ngày nghỉ có phép:</span>
              <span class="info-value-line" style="max-width:25mm;"></span>
              <span class="info-label" style="margin-left:4mm;">Số ngày nghỉ không phép:</span>
              <span class="info-value-line"></span>
            </div>
          </div>

          <div class="section-title" style="font-size:11pt;">1. Các môn học và hoạt động giáo dục</div>

          <!-- Bảng môn học -->
          <table>
            <thead>
              <tr>
                <th style="width:28%">Môn học và hoạt động giáo dục</th>
                <th style="width:16%">Mức đạt được</th>
                <th style="width:16%">Điểm KT ĐK</th>
                <th style="width:40%">Nhận xét</th>
              </tr>
            </thead>
            <tbody>
              ${this.buildSubjectRows()}
            </tbody>
          </table>

          <!-- Nhận xét cuối năm -->
          <div class="final-comment">
            <div class="final-label">6. Hoàn thành chương trình lớp học/chương trình tiểu học:</div>
            <div class="comment-lines">
              <span class="comment-line"></span>
              <span class="comment-line"></span>
              <span class="comment-line"></span>
            </div>
          </div>

          <!-- Chữ ký -->
          <div class="date-right">......................., ngày ....... tháng ....... năm 20......</div>
          <div class="dual-signature">
            <div class="sig-block">
              <div class="sig-title">Xác nhận của Hiệu trưởng</div>
              <div class="sig-note">(Ký, ghi rõ họ tên và đóng dấu)</div>
            </div>
            <div class="sig-block">
              <div class="sig-title">Giáo viên chủ nhiệm</div>
              <div class="sig-note">(Ký và ghi rõ họ tên)</div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  // ─── TABLE ROWS BUILDERS ────────────────────────────────────────────────────

  private buildLearningHistoryRows(student: HocSinhResponse): string {
    const yearRanges = [
      '20.... – 20....',
      '20.... – 20....',
      '20.... – 20....',
      '20.... – 20....',
      '20.... – 20....',
      '20.... – 20....',
      '20.... – 20....',
    ];

    // Fill first row with actual data if available
    const enrollment = student.enrollment;
    const rows = yearRanges.map((yr, idx) => {
      let yearCell = yr;
      let classCell = '';
      let schoolCell = '';
      let regCell = '';
      let dateCell = '';

      if (idx === 0 && enrollment) {
        yearCell = enrollment.schoolYearName ?? yr;
        classCell = enrollment.className ?? '';
        schoolCell = student.unitName ?? '';
        dateCell = this.formatDate(enrollment.enrolledAt);
      }

      return `<tr>
        <td>${yearCell}</td>
        <td>${classCell}</td>
        <td>${schoolCell}</td>
        <td>${regCell}</td>
        <td>${dateCell}</td>
      </tr>`;
    });
    return rows.join('');
  }

  private buildSubjectRows(): string {
    const subjects = [
      'Tiếng Việt',
      'Toán',
      'Ngoại ngữ 1\n......\n......',
      'Lịch sử và Địa lý',
      'Khoa học',
      'Tin học và Công nghệ\n(Tin học)',
      'Tin học và Công nghệ\n(Công nghệ)',
      'Đạo đức',
      'Tự nhiên và Xã hội',
      'Giáo dục thể chất',
      'Nghệ thuật\n(Âm nhạc)',
      'Nghệ thuật\n(Mỹ thuật)',
      'Hoạt động trải nghiệm',
      'Tiếng dân tộc',
    ];

    return subjects
      .map((subject) => {
        const subjectHtml = subject.replace(/\n/g, '<br/>');
        return `<tr>
          <td class="left">${subjectHtml}</td>
          <td></td>
          <td></td>
          <td>
            <div class="dot-lines">
              <span class="dot-line"></span>
              <span class="dot-line"></span>
            </div>
          </td>
        </tr>`;
      })
      .join('');
  }

  // ─── HELPER METHODS ─────────────────────────────────────────────────────────

  private getProvinceName(student: HocSinhResponse): string {
    const perm = this.getPermanentAddress(student);
    return perm?.provinceName ?? student.permanentProvinceName ?? '';
  }

  private getDistrictName(student: HocSinhResponse): string {
    const perm = this.getPermanentAddress(student);
    return perm?.districtName ?? '';
  }

  private getCurrentAddress(student: HocSinhResponse): string {
    const perm = this.getPermanentAddress(student);
    if (!perm) return '';
    const parts = [
      perm.detailAddress,
      perm.hamletName,
      perm.wardName,
      perm.districtName,
      perm.provinceName,
    ].filter(Boolean);
    return parts.join(', ');
  }

  private getPermanentAddress(student: HocSinhResponse): HocSinhAddress | undefined {
    return student.addresses?.find((a) =>
      (a.addressType ?? '').toLowerCase().includes('thuong')
    );
  }

  private getGuardianField(
    student: HocSinhResponse,
    type: string,
    field: keyof HocSinhGuardian
  ): string {
    const g = student.guardians?.find((x) =>
      (x.guardianType ?? '').toUpperCase().includes(type)
    );
    const val = g?.[field];
    return val ? `${val}` : '';
  }

  private getField(student: HocSinhResponse, key: string): string {
    const val = (student as any)[key];
    return val ? `${val}` : '';
  }

  private formatGender(student: HocSinhResponse): string {
    const val = student.gender;
    if (!val) return '';
    const s = String(val).toLowerCase();
    if (s === '0' || s === 'nam') return 'Nam';
    if (s === '1' || s === 'nu' || s === 'nữ') return 'Nữ';
    return String(val);
  }

  private formatDate(value?: string): string {
    if (!value) return '';
    const raw = value.slice(0, 10);
    const [year, month, day] = raw.split('-');
    if (!year || !month || !day) return raw;
    return `${day}/${month}/${year}`;
  }

  private formatDateFilename(): string {
    const now = new Date();
    const d = String(now.getDate()).padStart(2, '0');
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const y = now.getFullYear();
    return `${d}-${m}-${y}`;
  }
}
