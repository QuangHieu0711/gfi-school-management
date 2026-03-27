import { AbstractControl, FormGroup } from '@angular/forms';
import { MtxDatetimepickerType } from '@ng-matero/extensions/datetimepicker';
import { ID_TYPE } from './response.model';

export type IFormControl = Record<string, AbstractControl>;
export type ControlDirection = 'horizontal' | 'vertical';
export interface IOptions {
  value: ID_TYPE | boolean;
  label: string;
  disabled?: boolean;
  group?: string;
}
export class FormType {
  controlType: string;
  key: string;
  label?: string;
  showLabel?: boolean = true;
  hidden?: boolean = false;
  value?: unknown;
  type?: string;
  placeholder?: string;
  startPlaceholder?: string;
  endPlaceholder?: string;
  readOnly?: boolean = false;
  disabled?: boolean = false;
  hint?: string = '';
  autoFocus?: boolean = false;
  iconPrefix?: string = '';
  iconSuffix?: string = '';
  tabIndex?: number = 0;
  appendToBody?: boolean = false;

  // Validate rule
  required: boolean;
  max?: number;
  min?: number;
  maxLength?: number;
  minLength?: number;
  decimalPlaces?: number;
  maxDate?: string | Date;
  minDate?: string | Date;
  pattern?: string;
  regex?: RegExp;
  skipHtmlValidation?: boolean = false;
  // options
  options: IOptions[] = [];

  // ngselect
  clearable?: boolean = false;
  multiple?: boolean = false;
  maskCount?: number;
  addTag?: boolean = false;

  // checkbox
  checked?: boolean = false;
  direction?: ControlDirection = 'horizontal';

  // textarea
  rows?: number = 3;

  // datepicker
  dateType?: MtxDatetimepickerType = 'date';

  // file upload
  maxFileSize?: number = 2000000000; // MB
  maxTotalSize?: number = 2000000000; // MB
  accept?: string[] = []; // file type
  maxQuantity?: number = 2000000000; // number of file

  constructor(options: {
    controlType: string;
    key: string;
    label?: string;
    showLabel?: boolean;
    hidden?: boolean;
    value?: unknown;
    type?: string;
    placeholder?: string;
    startPlaceholder?: string;
    endPlaceholder?: string;
    readOnly?: boolean;
    disabled?: boolean;
    hint?: string;
    autoFocus?: boolean;
    iconPrefix?: string;
    iconSuffix?: string;
    tabIndex?: number;
    required: boolean;
    max?: number;
    min?: number;
    maxLength?: number;
    minLength?: number;
    decimalPlaces?: number;
    maxDate?: string | Date;
    minDate?: string | Date;
    pattern?: string;
    regex?: RegExp;
    skipHtmlValidation?: boolean;
    options?: IOptions[];
    clearable?: boolean;
    multiple?: boolean;
    maskCount?: number;
    addTag?: boolean;
    checked?: boolean;
    direction?: ControlDirection;
    rows?: number;
    dateType?: MtxDatetimepickerType;
    maxFileSize?: number;
    maxTotalSize?: number;
    accept?: string[];
    maxQuantity?: number;
  }) {
    this.controlType = options.controlType ?? '';
    this.key = options.key ?? '';
    this.label = options.label ?? '';
    this.showLabel = options.showLabel ?? true;
    this.hidden = options.hidden ?? false;
    this.value =
      options.controlType === 'select' && options.value == ''
        ? undefined
        : options.value;
    this.type = options.type ?? '';
    this.placeholder = options.placeholder ?? '';
    this.startPlaceholder = options.startPlaceholder ?? '';
    this.endPlaceholder = options.endPlaceholder ?? '';
    this.readOnly = options.readOnly;
    this.disabled = options.disabled;
    this.hint = options.hint;
    this.autoFocus = options.autoFocus;
    this.iconPrefix = options.iconPrefix;
    this.iconSuffix = options.iconSuffix;
    this.tabIndex = options.tabIndex ?? 0;
    this.required = options.required;
    this.max = options.max;
    this.min = options.min;
    this.maxLength = options.maxLength;
    this.minLength = options.minLength;
    this.decimalPlaces = options.decimalPlaces;
    this.maxDate = options.maxDate;
    this.minDate = options.minDate;
    this.pattern = options.pattern ?? '';
    this.regex = options.regex ?? undefined;
    this.skipHtmlValidation = options.skipHtmlValidation ?? false;
    this.options = options.options ?? [];
    this.clearable = options.clearable ?? false;
    this.multiple = options.multiple ?? false;
    this.maskCount = options.maskCount;
    this.addTag = options.addTag ?? false;
    this.checked = options.checked ?? false;
    this.direction = options.direction ?? 'horizontal';
    this.rows = options.rows;
    this.dateType = options.dateType;
    this.maxFileSize = options.maxFileSize ?? 2000000000;
    this.maxTotalSize = options.maxTotalSize ?? 2000000000;
    this.accept = options.accept ?? [];
    this.maxQuantity = options.maxQuantity ?? 2000000000;
  }
}

interface IControlOptions {
  controlName: string;
  label?: string;
  showLabel?: boolean;
  hidden?: boolean;
  defaultValue?: unknown;
  type?: string;
  placeholder?: string;
  startPlaceholder?: string;
  endPlaceholder?: string;
  readOnly?: boolean;
  disabled?: boolean;
  hint?: string;
  autoFocus?: boolean;
  iconPrefix?: string;
  iconSuffix?: string;
  tabIndex?: number;
  required: boolean;
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  decimalPlaces?: number;
  minDate?: string | Date;
  maxDate?: string | Date;
  pattern?: string;
  regex?: RegExp;
  skipHtmlValidation?: boolean;
  listOption?: IOptions[];
  clearable?: boolean;
  multiple?: boolean;
  maskCount?: number;
  addTag?: boolean;
  checked?: boolean;
  direction?: ControlDirection;
  rows?: number;
  dateType?: MtxDatetimepickerType;
  maxFileSize?: number;
  maxTotalSize?: number;
  accept?: string[];
  maxQuantity?: number;
}

export const TEXT_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'textbox',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    hidden: option.hidden ?? false,
    value: option.defaultValue,
    type: option.type,
    placeholder: option.placeholder,
    readOnly: option.readOnly,
    disabled: option.disabled,
    hint: option.hint,
    autoFocus: option.autoFocus,
    required: option.required,
    maxLength: option.maxLength,
    minLength: option.minLength,
    decimalPlaces: option.decimalPlaces,
    pattern: option.pattern,
    regex: option.regex,
    skipHtmlValidation: option.skipHtmlValidation,
    iconPrefix: option.iconPrefix,
    iconSuffix: option.iconSuffix,
    tabIndex: option.tabIndex,
  });

export const TEXTAREA_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'textarea',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    hidden: option.hidden ?? false,
    value: option.defaultValue,
    placeholder: option.placeholder,
    readOnly: option.readOnly,
    disabled: option.disabled,
    hint: option.hint,
    autoFocus: option.autoFocus,
    required: option.required,
    maxLength: option.maxLength,
    minLength: option.minLength,
    decimalPlaces: option.decimalPlaces,
    pattern: option.pattern,
    regex: option.regex,
    skipHtmlValidation: option.skipHtmlValidation,
    rows: option.rows,
    tabIndex: option.tabIndex,
  });

export const SELECT_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'select',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    hidden: option.hidden ?? false,
    value: option.defaultValue,
    placeholder: option.placeholder,
    readOnly: option.readOnly,
    disabled: option.disabled,
    hint: option.hint,
    autoFocus: option.autoFocus,
    required: option.required,
    options: option.listOption,
    clearable: option.clearable,
    multiple: option.multiple,
    maskCount: option.maskCount,
    addTag: option.addTag,
    tabIndex: option.tabIndex,
  });

export const RADIO_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'radio',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    value: option.defaultValue,
    placeholder: option.placeholder,
    disabled: option.disabled,
    required: option.required,
    options: option.listOption,
    hint: option.hint,
    direction: option.direction,
    tabIndex: option.tabIndex,
  });

export const CHECKBOX_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'checkbox',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    value: option.defaultValue,
    placeholder: option.placeholder,
    disabled: option.disabled,
    required: option.required,
    options: option.listOption,
    checked: option.checked,
    hint: option.hint,
    direction: option.direction,
    tabIndex: option.tabIndex,
  });

export const DATE_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'datetime',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    hidden: option.hidden ?? false,
    value: option.defaultValue,
    placeholder: option.placeholder,
    readOnly: option.readOnly,
    disabled: option.disabled,
    hint: option.hint,
    autoFocus: option.autoFocus,
    required: option.required,
    minDate: option.minDate,
    maxDate: option.maxDate,
    dateType: option.dateType,
    tabIndex: option.tabIndex,
  });

export const DATE_RANGE_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'daterange',
    key: option.controlName,
    label: option.label,
    showLabel: option.showLabel ?? true,
    hidden: option.hidden ?? false,
    value: option.defaultValue,
    placeholder: option.placeholder,
    startPlaceholder: option.startPlaceholder,
    endPlaceholder: option.endPlaceholder,
    readOnly: option.readOnly,
    disabled: option.disabled,
    hint: option.hint,
    autoFocus: option.autoFocus,
    required: option.required,
    minDate: option.minDate,
    maxDate: option.maxDate,
    dateType: option.dateType,
    tabIndex: option.tabIndex,
  });

export const FILE_CONTROL = (option: IControlOptions) =>
  new FormType({
    controlType: 'file',
    key: option.controlName,
    label: option.label,
    disabled: option.disabled,
    hint: option.hint,
    required: option.required,
    maxFileSize: option.maxFileSize,
    maxTotalSize: option.maxTotalSize,
    accept: option.accept,
    maxQuantity: option.maxQuantity,
    multiple: option.multiple,
  });

export interface FormChangedEvent {
  form: FormGroup;
  control: AbstractControl | null;
  $event: unknown;
  item: FormType;
}
