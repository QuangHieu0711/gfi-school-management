/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DndDropEvent, DndModule } from 'ngx-drag-drop';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { LanguageService } from '@service';
import { TranslateModule } from '@ngx-translate/core';
import { IconComponent } from '@components/app-icon/app-icon.component';

@Component({
  selector: 'app-file-control',
  templateUrl: './file-control.component.html',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ...MATERIAL_MODULE,
    DndModule,
    TranslateModule,
    IconComponent,
  ],
})
export class FileControlComponent extends FormGroupAbstractComponent {
  private static readonly MAX_TOTAL_SIZE_BYTES = 2 * 1024 * 1024 * 1024;

  @Input() files: File[] = [];
  @Input() isViewMode = false;
  @Input() enableMultiple?: boolean;
  @Input() maxTotalSize: number = FileControlComponent.MAX_TOTAL_SIZE_BYTES;
  @Output() filePreview = new EventEmitter<File>();

  constructor(protected override readonly languageService: LanguageService) {
    super(languageService);
  }

  onDrop({ event }: DndDropEvent): void {
    const droppedFiles = event.dataTransfer?.files;
    if (droppedFiles?.length) this.handleFiles(droppedFiles);
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input?.files?.length) this.handleFiles(input.files);
    input.files = null;
    input.value = '';
  }

  /**
   * Handles file selection and validation.
   * - Validates MIME type, file size, and total size.
   */
  handleFiles(fileList: FileList): void {
    const control = this.getControl();
    if (!control) return;

    // Clear previous errors
    control.setErrors(null);

    const incomingFiles = Array.from(fileList);

    // 1. Check multiple (optional)
    if (!this.enableMultiple && this.files.length + incomingFiles.length > 1) {
      control.setErrors({ multipleNotAllowed: true });
      control.markAsTouched();
      return;
    }

    // 2. Check duplicate file names (case-insensitive)
    const normalizedExistingNames = new Set(
      this.files.map((file) => file.name.trim().toLowerCase())
    );

    const normalizedIncomingNames = new Set<string>();
    const duplicateNames: string[] = [];

    for (const file of incomingFiles) {
      const normalizedName = file.name.trim().toLowerCase();

      if (
        normalizedExistingNames.has(normalizedName) ||
        normalizedIncomingNames.has(normalizedName)
      ) {
        duplicateNames.push(file.name);
      }

      normalizedIncomingNames.add(normalizedName);
    }

    if (duplicateNames.length > 0) {
      control.setErrors({ duplicateFileName: duplicateNames });
      control.markAsTouched();
      return;
    }

    // 3. Validate file type and individual size
    for (const file of incomingFiles) {
      if (!this.isAllowedFileType(file)) {
        control.setErrors({ invalidMIMEType: true });
        control.markAsTouched();
        return;
      }

      if (this.item?.maxFileSize && file.size > this.item.maxFileSize) {
        control.setErrors({ fileTooLarge: true });
        control.markAsTouched();
        return;
      }
    }

    // 4. Check max quantity (optional)
    const totalCount = this.files.length + incomingFiles.length;

    if (this.item?.maxQuantity && totalCount > this.item.maxQuantity) {
      control.setErrors({ maxQuantityExceeded: true });
      control.markAsTouched();
      return;
    }

    // 5. Check total file size (optional - boolean)
    if (this.maxTotalSize) {
      // 1. Check từng file
      for (const file of incomingFiles) {
        if (file.size > this.maxTotalSize) {
          control.setErrors({ fileTooLarge: true });
          control.markAsTouched();
          return;
        }
      }

      // 2. Check tổng dung lượng nhiều file
      const currentTotalSize = this.files.reduce((sum, f) => sum + f.size, 0);
      const newTotalSize = incomingFiles.reduce((sum, f) => sum + f.size, 0);
      const totalSize = currentTotalSize + newTotalSize;

      if (totalSize > this.maxTotalSize) {
        control.setErrors({ totalSizeExceeded: true });
        control.markAsTouched();
        return;
      }
    }

    // 6. All validations passed
    this.files = [...this.files, ...incomingFiles];

    this.setControlValue(this.files);
    this.emitValueChanged(this.files);
  }

  private isAllowedFileType(file: File): boolean {
    const accepts = (this.item.accept ?? [])
      .map((x) => String(x).trim().toLowerCase())
      .filter(Boolean);
    if (!accepts.length) return true;

    const fileName = String(file.name ?? '').toLowerCase();
    const dotIndex = fileName.lastIndexOf('.');
    const ext = dotIndex >= 0 ? fileName.slice(dotIndex) : '';
    const mime = String(file.type ?? '').toLowerCase();

    return accepts.some((rule) => {
      // Extension rule: ".pdf", ".xlsx", ".jpg"
      if (rule.startsWith('.')) return ext === rule;

      // Wildcard MIME rule: "image/*"
      if (rule.endsWith('/*')) {
        const prefix = rule.slice(0, -1);
        return mime.startsWith(prefix);
      }

      // Exact MIME rule: "application/pdf"
      if (rule.includes('/')) return mime === rule;

      // Fallback extension rule without dot: "pdf", "xlsx"
      return ext === `.${rule}`;
    });
  }

  removeFile(index: number): void {
    if (this.isViewMode) return; // Prevent deletion in view mode
    this.files.splice(index, 1);
    this.setControlValue(this.files);
    this.emitValueChanged(this.files);
  }

  previewFile(file: File | any): void {
    const realFile: File = file?.rawFile ?? file?.file ?? file;
    this.filePreview.emit(realFile);
  }

  getFileTranslate() {
    return this.item.multiple
      ? this.languageService.instant('formControl.fileUpload.fileType.multiple')
      : this.languageService.instant('formControl.fileUpload.fileType.single');
  }

  getFileSize(size: number): string {
    if (!Number.isFinite(size) || size < 0) return '0 B';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
    if (size < 1024 * 1024 * 1024)
      return `${(size / (1024 * 1024)).toFixed(2)} MB`;
    return `${(size / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  }

  /**
   * Get icon name based on file MIME type or extension
   */
  getFileIcon(file: File | any): string {
    const realFile: File = file?.rawFile ?? file?.file ?? file;

    const type = realFile?.type || '';
    const name = (realFile?.name || '').toLowerCase();

    const ext = name.includes('.') ? name.split('.').pop()! : '';

    const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];

    if (imageExts.includes(ext) || type.startsWith('image/')) return 'image';
    if (ext === 'pdf') return 'pdf';
    if (['doc', 'docx'].includes(ext)) return 'docx';
    if (['xls', 'xlsx', 'csv'].includes(ext)) return 'xlsx';
    if (['zip', '7z'].includes(ext)) return 'zip';
    if (ext === 'rar') return 'rar';

    return 'file';
  }
}
