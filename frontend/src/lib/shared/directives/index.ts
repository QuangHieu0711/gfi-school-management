import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { REGEX } from '@constant/constant';

export const ValidatorHTMLRegex = (): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const regex = REGEX.HTML_TAG_VALIDATE;
    regex.lastIndex = 0;
    return regex.test(control.value) ? { htmlPattern: true } : null;
  };
};

export const ValidatorRegex = (regex: RegExp): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    regex.lastIndex = 0;
    return !regex.test(control.value) ? { validatorRegex: true } : null;
  };
};
