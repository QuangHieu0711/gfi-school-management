import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { REGEX } from '@constant/constant';

/**
 * Validator that checks if the control value contains HTML tags.
 * Returns { htmlPattern: true } if any HTML tag is found.
 */
export const ValidatorHTMLRegex = (): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const regex = REGEX.HTML_TAG_VALIDATE;
    regex.lastIndex = 0;
    return regex.test(control.value) ? { htmlPattern: true } : null;
  };
};

/**
 * Generic validator that checks if the control value matches a given regex.
 * Returns { validatorRegex: true } if the value does not match.
 */
export const ValidatorRegex = (regex: RegExp): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    regex.lastIndex = 0;
    return !regex.test(control.value) ? { validatorRegex: true } : null;
  };
};

/**
 * Validator that trims the value and checks if it's non-empty.
 * Returns { required: true } if the trimmed value is empty.
 */
export const ValidatorRequired = (): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    const isEmpty =
      value === null ||
      value === undefined ||
      (typeof value === 'string' && value.trim() === '') ||
      (Array.isArray(value) && value.length === 0);

    return isEmpty ? { required: true } : null;
  };
};

/**
 * Validator that checks if the trimmed value length is less than the specified minimum.
 * Returns { minlengthTrim: { requiredLength, actualLength } } if the condition fails.
 */
export const ValidatorMinLengthTrim = (min: number): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const value = control.value.toString().trim();
    return value.length < min ? { minlengthTrim: { requiredLength: min, actualLength: value.length } } : null;
  };
};

/**
 * Validator that checks if the trimmed value length exceeds the specified maximum.
 * Returns { maxlengthTrim: { requiredLength, actualLength } } if the condition fails.
 */
export const ValidatorMaxLengthTrim = (max: number): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const value = control.value.toString().trim();
    return value.length > max ? { maxlengthTrim: { requiredLength: max, actualLength: value.length } } : null;
  };
};
