import { format } from 'date-fns';
import { sha256 as sha256Hash } from 'js-sha256';
import _ from 'lodash';
import { Observable, take } from 'rxjs';

/**
 * Utility functions for various operations.
 * This module provides functions for date formatting, UUID generation, number rounding,
 * JSON parsing, string manipulation, and more.
 */

/**
 * Converts a Date object to a formatted string based on the provided date format.
 * @param date The Date object to be formatted.
 * @param dateFormat The format string to use for formatting the date.
 * @returns A string representing the formatted date.
 */
export const converDateToFormat = (date: Date, dateFormat: string): string => format(date, dateFormat);

/**
 * Rounds a number to a specified number of decimal places.
 * @param number The number to round.
 * @param decimal The number of decimal places to round to (default is 2).
 * @returns The rounded number.
 */
export const roundNumber = (number: string | number, decimal = 2): number => {
  let floatNumber = 0;
  if (typeof number === 'string') floatNumber = parseFloat(number);
  else floatNumber = Number(number);

  if (isNaN(floatNumber)) return 0;

  if (!('' + floatNumber).includes('e')) {
    return +(Math.round(+(floatNumber + 'e+' + decimal)) + 'e-' + decimal);
  } else {
    const arr = ('' + floatNumber).split('e');
    let sig = '';
    if (+arr[1] + decimal > 0) sig = '+';

    return +(Math.round(+(arr[0] + 'e' + sig + (+arr[1] + decimal))) + 'e-' + decimal);
  }
};

/**
 * Formats a number with a specified separator for thousands.
 * @param string The number as a string to format.
 * @param separator The separator to use for thousands (default is '.').
 * @returns The formatted number as a string with the specified separator.
 */
export const numberWithSeparator = (value: string, separator = '.'): string => {
  const num = Number(value.replace(/[^0-9.-]/g, ''));
  if (isNaN(num)) return value;

  const formatted = new Intl.NumberFormat('en-US').format(num);
  return separator === ',' ? formatted : formatted.replace(/,/g, separator);
};

/**
 * Tries to parse a JSON string and returns the result or null if parsing fails.
 * @param value The JSON string to parse.
 * @returns The parsed JSON object or null if parsing fails.
 */
export const jSonTryParse = (value: string | null): unknown => {
  try {
    if (value) return JSON.parse(value);
    return value;
  } catch (e) {
    console.error('error try parsing json', e, value);
    if (value === 'undefined') return void 0;
    return null;
  }
};

/**
 * Removes accents from a string and converts it to lowercase.
 * @param string The string to de-accent and convert to lowercase.
 * @returns The de-accented and lowercased string.
 */
export const deAccent = (string: string): string => (string ? _.deburr(string.normalize('NFKD')).toLocaleLowerCase().trim() : '');

/**
 * Computes the SHA-256 hash of a given message.
 * @param message The message to hash.
 * @returns A promise that resolves to the SHA-256 hash of the message as a hexadecimal string.
 */
export const sha256 = async (message: string): Promise<string> => sha256Hash(message);

/**
 * Get the value from an observable.
 * This method subscribes to the observable and returns the first emitted value.
 * @param obs The observable to get the value from.
 * @returns The first emitted value from the observable.
 */
export const getObsValue = <T>(obs: Observable<T>): T => {
  let value: T = {} as T; // Initialize with an empty object of type T
  obs.pipe(take(1)).subscribe((res) => (value = res));
  return value;
};

/**
 * Copies a given text to the clipboard.
 * This function uses the Clipboard API to write text to the clipboard.
 * @param text The text to copy to the clipboard.
 * @returns A promise that resolves when the text has been copied successfully.
 */
export const copyToClipboard = async (text: string): Promise<void> => {
  try {
    await navigator.clipboard.writeText(text);
  } catch (err) {
    console.error('Failed to copy: ', err);
  }
};

