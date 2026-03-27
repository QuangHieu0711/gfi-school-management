/* eslint-disable @typescript-eslint/no-explicit-any */
import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

@Injectable({ providedIn: 'root' })
export class ApiErrorToastService {
  constructor(private toastr: ToastrService) {}

  private getErrorData(error: any): any {
    // If already the body (has code directly), return as-is
    if (error?.code !== undefined) return error;
    // HttpErrorResponse has error body in error.error
    return error?.error || error;
  }

  private stripFailurePrefix(message: string): string {
    return message.replace(/^thất bại!\s*/i, '').trim();
  }

  handleDependencyDelete(error: any, entityName?: string): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 3200) return false;
    const raw = entityName
      ? `${entityName} đang có tài liệu hồ sơ, vui lòng kiểm tra lại`
      : errorData?.userMessage ||
        errorData?.message ||
        'Xóa thất bại do đang có tài liệu hồ sơ, vui lòng kiểm tra lại';
    this.toastr.error(raw, 'Thất bại');
    return true;
  }

  handleCreateUpdate(error: any, entityName?: string): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 3005) return false;
    const message =
      errorData?.userMessage ||
      errorData?.message ||
      (entityName
        ? `Tên ${entityName} đã tồn tại`
        : 'Lưu thất bại do đã tồn tại');
    this.toastr.error(message, 'Thất bại');
    return true;
  }

  handleTaskIdAlreadyExists(
    error: any,
    entityName = 'công tác đo vẽ'
  ): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 3006) return false;
    const message =
      errorData?.userMessage ||
      errorData?.message ||
      `Mã ${entityName} đã tồn tại.`;
    this.toastr.error(message, 'Thất bại');
    return true;
  }

  handleConstructionIdAlreadyExists(
    error: any,
    entityName = 'công trình khai đào'
  ): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 3007) return false;
    const message =
      errorData?.userMessage ||
      errorData?.message ||
      `Mã ${entityName} đã tồn tại.`;
    this.toastr.error(message, 'Thất bại');
    return true;
  }

  handleDossierNameAlreadyExists(error: any): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 3008) return false;
    const message =
      errorData?.userMessage || errorData?.message || `Tên hồ sơ đã tồn tại.`;
    this.toastr.error(message, 'Thất bại');
    return true;
  }
  handleFileSizeExceeded(error: any, maxSize = '2GB'): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 2028) return false;
    const message =
      errorData?.userMessage ||
      errorData?.message ||
      `Không được tải lên file có dung lượng quá ${maxSize}`;
    this.toastr.error(message, 'Thất bại');
    return true;
  }

  handleInvalidFileFormat(error: any, acceptedFormats?: string): boolean {
    const errorData = this.getErrorData(error);
    if (errorData?.code !== 2021) return false;
    const message =
      errorData?.userMessage ||
      errorData?.message ||
      (acceptedFormats
        ? `Nội dung file không khớp với định dạng được khai báo. Chỉ chấp nhận: ${acceptedFormats}`
        : 'Nội dung file không khớp với định dạng được khai báo');
    this.toastr.error(message, 'Thất bại');
    return true;
  }

  handleDefault(error: any, fallback = 'Có lỗi xảy ra'): void {
    const errorData = this.getErrorData(error);
    this.toastr.error(
      errorData?.userMessage || errorData?.message || fallback,
      'Thất bại'
    );
  }
}
