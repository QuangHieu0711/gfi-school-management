import {
  Directive,
  EventEmitter,
  Input,
  Output,
  AfterViewInit,
  ViewChild,
  ElementRef,
  inject,
} from '@angular/core';
import { AbstractControl, FormGroup, ValidationErrors } from '@angular/forms';
import { MatSelectChange } from '@angular/material/select';
import { MtxDatetimepickerInputEvent } from '@ng-matero/extensions/datetimepicker';
import {
  FormChangedEvent,
  FormType,
  IFormControl,
} from '@model/form-control.model';
import { LanguageService } from '@service';

@Directive()
export abstract class FormGroupAbstractComponent implements AfterViewInit {
  @Input() item!: FormType;
  @Input() form: FormGroup = new FormGroup({});
  @Input() appendToBody = false;

  @ViewChild('nativeElement') nativeElement!: ElementRef<HTMLElement>;

  @Output() valueChanged = new EventEmitter<FormChangedEvent>();

  @Output() controlFocus = new EventEmitter<FocusEvent>();
  @Output() controlFocusOut = new EventEmitter<FocusEvent>();
  @Output() controlBlur = new EventEmitter<FocusEvent>();

  private readonly elRef = inject(ElementRef);

  get isInsideDialog(): boolean {
    return !!this.elRef.nativeElement.closest('.mat-mdc-dialog-panel');
  }

  /** Convenient getter for controls */
  get f(): IFormControl {
    return this.form.controls;
  }

  constructor(protected readonly languageService: LanguageService) {}

  /**
   * Lifecycle hook to handle auto-focus and disabled state
   */
  ngAfterViewInit(): void {
    // Auto-focus support
    if (this.shouldAutoFocus()) this.focus();
    if (this.isDisabled()) {
      this.disable();
    }
  }

  /**
   * Programmatically focus the input field
   */
  focus(): void {
    if (this.nativeElement?.nativeElement)
      setTimeout(() => this.nativeElement.nativeElement.focus(), 0);
  }

  /**
   * Programmatically disable the input field
   */
  disable(): void {
    const control = this.getControl();
    if (!control) return;
    setTimeout(() => control.disable(), 0);
  }

  /** Emit when value changes */
  emitValueChanged($event: unknown): void {
    this.valueChanged.emit({
      form: this.form,
      control: this.getControl(),
      $event,
      item: this.item,
    });
  }

  /** Check if the control is touched and has a specific or general error */
  isInvalidControl(errorKey?: string): boolean {
    const control = this.getControl();
    if (!control) return false;

    return (
      control.touched &&
      (errorKey ? control.hasError(errorKey) : control.invalid)
    );
  }

  /** Returns a readable error message */
  getErrorMessage(): string | null {
    const control = this.getControl();
    if (!control?.errors || !control.touched) return null;

    const errors: ValidationErrors = control.errors;
    const message = this.resolveErrorMessage(errors);
    if (message) return message;

    // Custom error support (like regex)
    if (this.item.regex && !this.item.regex.test(control.value))
      return this.languageService.instant(
        'formControl.validateMessage.customRegex'
      );

    return this.languageService.instant('formControl.validateMessage.invalid');
  }

  private resolveErrorMessage(errors: ValidationErrors): string | null {
    if (
      errors['matDatepickerParse'] ||
      errors['mtxDatetimepickerParse'] ||
      errors['matDatetimepickerParse']
    ) {
      return this.languageService.instant(
        'formControl.validateMessage.invalid'
      );
    }

    if (errors['required']) {
      return this.languageService.instant(
        'formControl.validateMessage.required',
        { field: this.item.label }
      );
    }
    if (errors['minlength']) {
      return this.languageService.instant(
        'formControl.validateMessage.minlength',
        {
          requiredLength: errors['minlength'].requiredLength,
          field: this.item.label,
        }
      );
    }
    if (errors['minlengthTrim']) {
      return this.languageService.instant(
        'formControl.validateMessage.minlength',
        {
          requiredLength: errors['minlengthTrim'].requiredLength,
          field: this.item.label,
        }
      );
    }
    if (errors['maxlength']) {
      return this.languageService.instant(
        'formControl.validateMessage.maxlength',
        {
          requiredLength: errors['maxlength'].requiredLength,
          field: this.item.label,
        }
      );
    }
    if (errors['maxlengthTrim']) {
      return this.languageService.instant(
        'formControl.validateMessage.maxlength',
        {
          requiredLength: errors['maxlengthTrim'].requiredLength,
          field: this.item.label,
        }
      );
    }
    if (errors['min'])
      return this.languageService.instant('formControl.validateMessage.min', {
        min: errors['min'].min,
      });
    if (errors['max'])
      return this.languageService.instant('formControl.validateMessage.max', {
        max: errors['max'].max,
      });
    if (errors['float']) {
      return `${this.item.label} phải là số thực hợp lệ.`;
    }
    if (errors['dateFuture']) {
      return errors['dateFuture'];
    }

    if (errors['dateInvalid']) {
      return errors['dateInvalid'];
    }

    if (errors['range']) {
      return errors['range'];
    }
    if (errors['pattern'])
      return this.languageService.instant(
        'formControl.validateMessage.pattern'
      );
    if (errors['email'])
      return this.languageService.instant('formControl.validateMessage.email');
    if (errors['phone'])
      return this.languageService.instant('formControl.validateMessage.phone');
    if (errors['number'])
      return this.languageService.instant('formControl.validateMessage.number');
    if (errors['text'])
      return this.languageService.instant('formControl.validateMessage.text');
    if (errors['numberPositive'])
      return this.languageService.instant(
        'formControl.validateMessage.numberPositive'
      );

    if (errors['passwordMismatch']) return 'Mật khẩu xác nhận không khớp';

    // 🔽 File-related errors
    if (errors['invalidMIMEType']) {
      const allowed = (this.item.accept ?? [])
        .map((x) => String(x).trim())
        .filter(Boolean)
        .join(', ');
      return allowed
        ? `Định dạng file được phép tải lên ${allowed}`
        : this.languageService.instant(
            'formControl.validateMessage.invalidMIMEType'
          );
    }
    if (errors['fileTooLarge'])
      return this.languageService.instant(
        'formControl.validateMessage.fileTooLarge'
      );
    if (errors['totalSizeExceeded'])
      return this.languageService.instant(
        'formControl.validateMessage.totalSizeExceeded'
      );
    if (errors['maxQuantityExceeded'])
      return this.languageService.instant(
        'formControl.validateMessage.maxQuantityExceeded'
      );
    if (errors['duplicateFileName']) {
      const duplicateNames = Array.isArray(errors['duplicateFileName'])
        ? errors['duplicateFileName'].join(', ')
        : '';
      return duplicateNames
        ? `Tên file đã tồn tại: ${duplicateNames}`
        : 'Tên file đã tồn tại';
    }

    return null;
  }

  /** Retrieves a control from the form using the item key */
  getControl(): AbstractControl | null {
    const control = this.f?.[this.item?.key];
    if (!control) {
      console.warn(`Control not found: ${this.item?.key}`);
      return null;
    }
    return control;
  }

  /** Determine if the control should be disabled */
  isDisabled(): boolean {
    return (
      !!this.item?.disabled ||
      !this.getControl() ||
      !!this.getControl()?.disabled
    );
  }

  /** Determine if the control is readonly */
  isReadOnly(): boolean {
    return !!this.item?.readOnly;
  }

  /** Determine if the control should auto focus */
  shouldAutoFocus(): boolean {
    return !!this.item?.autoFocus;
  }

  /** Determine if the control is required */
  isRequired(): boolean {
    return !!this.item?.required;
  }

  /** Get current value safely */
  getControlValue(): string | string[] | Date {
    const control = this.getControl();
    return control?.getRawValue() ?? null;
  }

  /** Utility: Set a value programmatically */
  setControlValue(value: string | string[] | Date | File | File[]): void {
    const control = this.getControl();
    if (control) {
      control.setValue(value);
      control.markAsTouched();
    }
  }

  /** Utility: Reset control */
  resetControl(): void {
    const control = this.getControl();
    if (control) {
      control.reset();
    }
  }

  /**
   * Handles input change
   */
  onChange(event: Event): void {
    this.emitValueChanged(event);
  }

  /**
   * Handles date change
   */
  onChangeDate(event: MtxDatetimepickerInputEvent<Date>): void {
    this.emitValueChanged(event);
  }

  /**
   * Handles select change
   */
  onSelectChange(event: MatSelectChange): void {
    this.emitValueChanged(event);
  }

  /**
   * Handles focus
   */
  onFocus(event: FocusEvent): void {
    this.controlFocus.emit(event);
  }

  /**
   * Handles focus out, trims value
   */
  onFocusOut(event: FocusEvent): void {
    this.controlFocusOut.emit(event);
  }

  /**
   * Handles blur
   */
  onBlur(event: FocusEvent): void {
    const control = this.getControl();
    if (control && control.value && typeof control.value === 'string')
      control.patchValue(control.value.trim(), { emitEvent: true });
    this.controlBlur.emit(event);
  }

  trackByFn(item: unknown): unknown {
    return (item as { value: unknown }).value;
  }
}
