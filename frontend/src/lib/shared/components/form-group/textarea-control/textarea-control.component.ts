import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-textarea-control',
  templateUrl: './textarea-control.component.html',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, ...MATERIAL_MODULE],
})
export class TextareaControlComponent extends FormGroupAbstractComponent implements OnInit {
  private readonly defaultMaxLength = 255;
 
  ngOnInit(): void {
    const control = this.getControl();
    if (control) {
      control.addValidators(
        Validators.maxLength(this.getEffectiveMaxLength())
      );
      control.updateValueAndValidity({ emitEvent: false });
    }
  }
 
  private getEffectiveMaxLength(): number {
    const maxLength = Number(this.item.maxLength);
    return Number.isFinite(maxLength) && maxLength > 0
      ? maxLength
      : this.defaultMaxLength;
  }
 
  getLabel(): string | undefined {
    return this.isReadOnly() || this.isDisabled()
      ? (this.getControlValue() as string)
      : undefined;
  }

  isMaxLengthReached(): boolean {
    if (this.isReadOnly() || this.isDisabled()) {
      return false;
    }

    const control = this.getControl();
    const rawValue = control?.value;
    const value = rawValue == null ? '' : String(rawValue);
    return !!control?.dirty && value.length > this.getEffectiveMaxLength();
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
    if (!this.item.maxLength) return;
    this.getControl()?.markAsDirty();
  }
}
