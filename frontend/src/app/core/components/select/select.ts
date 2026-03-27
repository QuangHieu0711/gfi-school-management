import { Component, effect, forwardRef, Input } from '@angular/core';
import { SelectModule } from 'primeng/select';

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
  selector: 'app-core-select',
  imports: [
    SelectModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './select.html',
  styleUrl: './select.scss',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CoreSelect),
      multi: true
    }
  ]
})
export class CoreSelect implements ControlValueAccessor {
  @Input() isShowLabel = true;
  @Input() labelName = '';
  @Input() placeholder = '';
  @Input() options: any[] = [];
  @Input() appendTo: 'body' | 'self' = 'body';
  @Input() required = false;
  @Input() panelStyleClass = '';

  ctrl: FormControl = new FormControl();
  optionLabel = 'name';
  optionValue = 'id';
  value: string | nullish = null;

  private initControlEffect = effect(() => {
    // const emailRegex = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
    this.ctrl = new FormControl(null, {
      validators: this.required === true ? Validators.required : Validators.nullValidator
    });

    this.ctrl.markAsDirty();
    this.ctrl.valueChanges.pipe(distinctUntilChanged()).subscribe((val: any) => {
      let value = this.options.filter(fil => fil[this.optionValue] === val);
      if (value && value.length > 0) {
        this.onChange(value[0]);
      } else {
        this.onChange(null);
      }
    });
  })

  // callbacks Angular Form truyền vào
  private onChange = (value: any) => { };
  private onTouched = () => { };

  // Form → Component
  writeValue(value: any): void {
    if (value === null) {
      this.ctrl.setValue(null);
    } else {
      this.findDataLut(value);
    }
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


  private findDataLut(val: any) {
    let dFilter: any = null;
    if (typeof (val) === 'object') {
      dFilter = this.options.filter((ds: any) => val[this.optionValue] && ds[this.optionValue] && ds[this.optionValue].toString() === val[this.optionValue].toString());
    } else {
      dFilter = this.options.filter((ds: any) => val && ds[this.optionValue] && ds[this.optionValue].toString() === val.toString());
    }
    if (dFilter && dFilter.length > 0) {
      this.ctrl.setValue(dFilter[0]);
    } else {
      this.ctrl.setValue(null);
    }
  }

}
