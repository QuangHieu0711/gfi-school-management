import { Component, effect, forwardRef, input, Input, OnInit } from '@angular/core';
import { RadioButtonModule } from 'primeng/radiobutton';

import {
  ControlValueAccessor,
  FormControl,
  FormsModule,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-radio-button',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RadioButtonModule
  ],
  templateUrl: './radio-button.html',
  styleUrl: './radio-button.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CoreRadioButton),
      multi: true
    }
  ]
})
export class CoreRadioButton implements ControlValueAccessor, OnInit {
  isShowLabel = input(true);
  isCtrlBlur = input(true);
  labelName = input('');
  placeholder = input('');
  options = input<any>([]);
  required = input(false);

  ctrl: FormControl = new FormControl();
  optionLabel = 'name';
  optionValue = 'id';
  value: string | nullish = null;

  // callbacks Angular Form truyền vào
  private onChange = (value: any) => { };
  private onTouched = () => { };

  ngOnInit(): void {
    // const emailRegex = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
    this.ctrl = new FormControl(null, {
      validators: this.required() === true ? Validators.required : Validators.nullValidator
    });

    this.ctrl.markAsDirty();
    this.ctrl.valueChanges.pipe(distinctUntilChanged()).subscribe((val: any) => {
      let value = this.findDataLut(val);
      this.onChange(value);
    });
  }

  // Form → Component
  writeValue(value: any): void {
    if (value === null) {
      this.ctrl.setValue(null);
    } else {
      this.findDataLut(value);
    }
  }

  private findDataLut(val: any) {
    let dFilter: any = null; let data = null;
    if (typeof (val) === 'object') {
      dFilter = this.options().filter((ds: any) => val[this.optionValue] && ds[this.optionValue] && ds[this.optionValue].toString() === val[this.optionValue].toString());
    } else {
      dFilter = this.options().filter((ds: any) => val && ds[this.optionValue] && ds[this.optionValue].toString() === val.toString());
    }
    if (dFilter && dFilter.length > 0) {
      data = dFilter[0]
      this.ctrl.setValue(data, { emitEvent: false });
    } else {
      this.ctrl.setValue(null, { emitEvent: false });
    }

    return data;
  }

  // Component → Form
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  onInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.value = value;
    this.onChange(value); // QUAN TRỌNG
  }


}
