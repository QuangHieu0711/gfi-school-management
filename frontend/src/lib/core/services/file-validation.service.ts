import { Injectable } from '@angular/core';

export interface FileCheckResult {
  valid: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class FileValidationService {
  private allowedExtensions = ['xlsx', 'xls'];

  checkFile(file: File): FileCheckResult {
    const fileName = file.name;
    const fileExtension = fileName.split('.').pop()?.toLowerCase();

    if (!this.allowedExtensions.includes(fileExtension!)) {
      return {
        valid: false,
        message: `Chỉ cho phép file Excel (.xlsx, .xls)! File của bạn là .${fileExtension}`,
      };
    }

    return {
      valid: true,
      message: `File hợp lệ: ${fileName}`,
    };
  }

  getAllowedExtensions(): string[] {
    return this.allowedExtensions;
  }
}
