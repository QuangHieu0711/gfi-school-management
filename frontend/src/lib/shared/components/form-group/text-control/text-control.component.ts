import { Component, Input, inject, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule, DatePipe } from '@angular/common';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { LanguageService } from '@service';

@Component({
  selector: 'app-text-control',
  templateUrl: './text-control.component.html',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    CommonModule,
    ...MATERIAL_MODULE,
    IconComponent,
  ],
  providers: [DatePipe],
})
export class TextControlComponent
  extends FormGroupAbstractComponent
  implements OnInit
{
  @Input() dateFormat?: string;
  @Input() suppressErrorMessage = false;

  private readonly defaultMaxLength = 255;
  private datePipe = inject(DatePipe);
  protected override languageService = inject(LanguageService);

  ngOnInit(): void {
    const control = this.getControl();
    if (control) {
      control.addValidators(Validators.maxLength(this.getEffectiveMaxLength()));
      control.updateValueAndValidity({ emitEvent: false });
    }
  }

  private getEffectiveMaxLength(): number {
    const maxLength = Number(this.item.maxLength);
    return Number.isFinite(maxLength) && maxLength > 0
      ? maxLength
      : this.defaultMaxLength;
  }

  override getErrorMessage(): string {
    const control = this.getControl();
    const label = this.item.label;

    if (this.item.type === 'password') {
      if (control?.hasError('required')) {
        return `${label} là trường bắt buộc`;
      }
      if (control?.hasError('pattern') || control?.invalid) {
        return `${label} tối thiểu 8 ký tự, bao gồm chữ viết hoa, chữ viết thường, ký tự đặc biệt và ký tự số`;
      }
    }

    if (control?.hasError('maxlength')) {
      return this.getMaxLengthMessage();
    }

    if (control?.hasError('decimalPlaces')) {
      return this.languageService.instant(
        'formControl.validateMessage.decimal',
        {
          decimalPlaces: this.item.decimalPlaces ?? 2,
          field: label,
        }
      );
    }

    if (control?.hasError('decimalInvalid')) {
      return `${label} không đúng định dạng số`;
    }

    return super.getErrorMessage() ?? '';
  }

  get formattedValue(): string {
    if (this.dateFormat) {
      const value = this.getControlValue();
      if (value instanceof Date || typeof value === 'string') {
        return this.datePipe.transform(value, 'dd/MM/yyyy') || '';
      }
    }
    return this.getControlValue() as string;
  }

  getLabel(): string | undefined {
    return this.isReadOnly() || this.isDisabled()
      ? (this.getControlValue() as string)
      : undefined;
  }

  getIconColorClass(): string {
    const control = this.getControl();
    if (!control?.touched && !control?.dirty && !control?.value) {
      return '!text-neutral-450';
    } else if (this.isInvalidControl()) {
      return '!text-error-500';
    } else {
      return '!text-dark-500';
    }
  }

  isMaxLengthReached(): boolean {
    if (this.isReadOnly() || this.isDisabled()) {
      return false;
    }
    const control = this.getControl();
    return !!control?.hasError('maxlength');
  }

  getMaxLengthMessage(): string {
    const key = this.item.label
      ? 'formControl.validateMessage.maxlength'
      : 'formControl.validateMessage.maxlength_no_field';

    return this.languageService.instant(key, {
      requiredLength: this.getEffectiveMaxLength(),
      field: this.item.label,
    });
  }

  onPaste(event: ClipboardEvent): void {
    // if (!this.item.maxLength) return;
    // this.getControl()?.markAsDirty();
  }
}
