import {
  Component,
  computed,
  DestroyRef,
  effect,
  forwardRef,
  inject,
  Input,
  input,
  OnInit,
  signal,
} from '@angular/core';
import {
  FormControl,
  FormsModule,
  NgModel,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { distinctUntilChanged } from 'rxjs';

export type InputType = 'text' | 'textarea' | 'number';

@Component({
  selector: 'app-input',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    InputNumberModule,
  ],
  templateUrl: './input.html',
  styleUrl: './input.scss',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CoreInput),
      multi: true,
    },
  ],
})
export class CoreInput implements OnInit, ControlValueAccessor {
  type = input<InputType>();
  isShowLabel = input(true);
  isCtrlBlur = input(true);
  labelName = input('');
  placeholder = input('');
  required = input(false);

  isInputText = computed(() => this.type() === 'text');
  isInputTextarea = computed(() => this.type() === 'textarea');
  isInputNumber = computed(() => this.type() === 'number');

  disabled = false;

  ctrl!: FormControl;

  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.ctrl = new FormControl(null, {
      updateOn: this.isCtrlBlur() ? 'blur' : 'change',
    });
    this.ctrl.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((val) => {
        this.onChange(val);
      });
  }

  private requiredEffect = effect(() => {
    const required = this.required();

    this.ctrl.setValidators(
      required ? Validators.required : Validators.nullValidator
    );

    this.ctrl.updateValueAndValidity({ emitEvent: false });
  });

  // callbacks Angular Form truyền vào
  private onChange: (value: any) => void = () => {};
  private onTouched: () => void = () => {};

  // Form → Component
  writeValue(value: any): void {
    this.ctrl.setValue(value ?? '', { emitEvent: false });
  }

  // Component → Form
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
}
