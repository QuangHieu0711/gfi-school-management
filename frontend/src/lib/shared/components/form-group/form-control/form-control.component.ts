import {
  Component,
  EventEmitter,
  Input,
  Output,
  ElementRef,
  AfterViewInit,
} from '@angular/core';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-form-control',
  templateUrl: './form-control.component.html',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, ...MATERIAL_MODULE],
})
export class FormControlComponent implements AfterViewInit {
  @Input() form: FormGroup = new FormGroup({});
  @Output() submitEvent = new EventEmitter();
  @Output() resetEvent = new EventEmitter();

  constructor(private el: ElementRef) {}

  ngAfterViewInit(): void {
    this.focusFirstInput();
  }

  focusFirstInput() {
    setTimeout(() => {
      const selectors = [
        'input:not([disabled]):not([readonly]):not([type="hidden"])',
        'textarea:not([disabled]):not([readonly])',
        'mtx-select:not(.mtx-select-disabled) .mtx-select-container',
        'mat-select:not([disabled]) .mat-mdc-select-trigger',
      ].join(', ');

      const candidates = this.el.nativeElement.querySelectorAll(selectors);

      const candidateArray = Array.from(candidates) as HTMLElement[];

      const firstEnabledVisible = candidateArray.find((el) => {
        const rect = el.getBoundingClientRect();
        const isVisible = rect.width > 0 && rect.height > 0;

        const style = window.getComputedStyle(el);
        return (
          isVisible && style.display !== 'none' && style.visibility !== 'hidden'
        );
      });

      if (firstEnabledVisible) {
        firstEnabledVisible.focus();
      }
    }, 500);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.form.markAllAsDirty();
      this.form.updateValueAndValidity();

      this.focusFirstInvalid();
      return;
    }

    this.submitEvent.emit(this.form.getRawValue());
  }

  focusFirstInvalid() {
    setTimeout(() => {
      const selectors = [
        'input.ng-invalid',
        'textarea.ng-invalid',
        'select.ng-invalid',
        'mtx-select.ng-invalid input',
        'mtx-select.ng-invalid .mtx-select-container',
        'mat-select.ng-invalid .mat-mdc-select-trigger',
        '.mat-mdc-form-field-invalid .mat-mdc-select-trigger',
      ];

      const element = this.el.nativeElement.querySelector(selectors.join(', '));

      if (element) {
        const el = element as HTMLElement;

        el.focus();
        if (el.closest('mtx-select')) {
          el.click();
        }

        el.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
        });
      }
    });
  }

  reset() {
    this.form.reset();
    this.resetEvent.emit();
  }
}
