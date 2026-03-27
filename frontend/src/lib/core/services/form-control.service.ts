import { Injectable } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {
  ValidatorHTMLRegex,
  ValidatorMaxLengthTrim,
  ValidatorMinLengthTrim,
  ValidatorRegex,
  ValidatorRequired,
} from '@validators';
import { FormType } from '@model/form-control.model';

@Injectable({
  providedIn: 'root',
})
export class ItemControlService {
  toFormGroup(items: FormType[], customValidator = null) {
    const group: Record<string, AbstractControl> = {};
    items.forEach((item) => {
      if (item.controlType === 'template') return;

      // Handle date range controls by creating two separate controls
      if (item.controlType === 'daterange') {
        const keys = item.key.split('-');
        const startKey = keys[0] || 'startDate';
        const endKey = keys[1] || 'endDate';
        const validate: ValidatorFn[] = this.validateControl(item);
        const value = item.value as Record<string, unknown>;

        group[startKey] = new FormControl(
          { value: value?.['start'] || null, disabled: !!item.disabled },
          validate
        );
        group[endKey] = new FormControl(
          { value: value?.['end'] || null, disabled: !!item.disabled },
          validate
        );
        return;
      }

      const validate: ValidatorFn[] = this.validateControl(item);
      group[item.key] = new FormControl(
        { value: item.value, disabled: !!item.disabled },
        validate
      );
    });
    return new FormGroup(group, { validators: customValidator });
  }

  toFormControl(items: FormType[]) {
    let control = null;
    items.forEach((item) => {
      if (item.controlType === 'template') return;
      const validate: ValidatorFn[] = this.validateControl(item);
      control = new FormControl(
        { value: item.value, disabled: !!item.disabled },
        validate
      );
    });
    return control;
  }

  validateControl(item: FormType) {
    const validate: ValidatorFn[] = [];
    if (item.min) validate.push(Validators.min(Number(item.min)));
    if (item.max) validate.push(Validators.max(Number(item.max)));
    if (item.minLength)
      validate.push(ValidatorMinLengthTrim(Number(item.minLength)));
    if (item.maxLength)
      validate.push(ValidatorMaxLengthTrim(Number(item.maxLength)));
    if (item.required) validate.push(ValidatorRequired());
    if (item.regex) validate.push(ValidatorRegex(item.regex));
    if (item.controlType === 'textbox' && item.type === 'email')
      validate.push(Validators.email);
    if (
      ['textarea', 'textbox'].includes(item.controlType) &&
      !item.skipHtmlValidation
    ) {
      if (item.decimalPlaces != null) {
        validate.push(this.decimalValidator(item.decimalPlaces));
      }
      validate.push(ValidatorHTMLRegex());
    }
    return validate;
  }

  addFormArrayToGroup(key: string, form: FormGroup) {
    return form.addControl(key, new FormArray([]));
  }

  getFormArrayFromGroupByKey(key: string, form: FormGroup) {
    return form.controls[key] as FormArray;
  }

  decimalValidator(decimalPlaces: number): ValidatorFn {
    return (control: AbstractControl) => {
      const value = control.value;

      if (!value) return null;

      const str = String(value);

      if (!str.includes('.')) return null;

      const parts = str.split('.');

      // nhiều dấu .
      if (parts.length > 2) {
        return { decimalInvalid: true };
      }

      // quá số thập phân
      if (parts[1] && parts[1].length > decimalPlaces) {
        return { decimalPlaces: true };
      }

      return null;
    };
  }
}
