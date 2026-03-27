import { format } from 'date-fns';
import _ from 'lodash';

export const getDataLocalStorageByKey = (key: string): unknown => {
  const item = localStorage.getItem(key);
  if (item && item !== 'null') {
    return JSON.parse(item);
  } else {
    return [];
  }
};

export const getDataSessionStorageByKey = (key: string): unknown => {
  const item = sessionStorage.getItem(key);
  if (item && item !== 'null') {
    return JSON.parse(item);
  } else {
    return [];
  }
};

export const converDateToFormat = (date: Date, dateFormat: string): string => format(date, dateFormat);

export const generateUUID = () => {
  let d = new Date().getTime();
  let d2 = (typeof performance !== 'undefined' && performance.now && performance.now() * 1000) || 0; //Time in microseconds since page-load or 0 if unsupported
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    let r = Math.random() * 16; //random number between 0 and 16
    if (d > 0) {
      //Use timestamp until depleted
      r = (d + r) % 16 | 0;
      d = Math.floor(d / 16);
    } else {
      //Use microseconds since page-load if supported
      r = (d2 + r) % 16 | 0;
      d2 = Math.floor(d2 / 16);
    }
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
};

// Precise rounding decimal
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

export const numberWithSeparator = (string: string, separator = '.'): string => {
  return string.toString().replace(/\B(?=(\d{3})+(?!\d))/g, separator);
};

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

export const deAccent = (string: string): string => (string ? _.deburr(string.normalize('NFKD')).toLocaleLowerCase().trim() : '');
