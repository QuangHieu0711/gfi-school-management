/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  MtxCalendarView,
  MtxDatetimepickerModule,
} from '@ng-matero/extensions/datetimepicker';
import {
  DateFnsDateTimeAdapter,
  MtxDateFnsDatetimeModule,
} from '@ng-matero/extensions-date-fns-adapter';
import {
  DateAdapter,
  MAT_DATE_LOCALE,
  MAT_DATE_FORMATS,
} from '@angular/material/core';
import { DateFnsAdapter } from '@angular/material-date-fns-adapter';
import {
  DatetimeAdapter,
  MTX_DATETIME_FORMATS,
} from '@ng-matero/extensions/core';
import { vi } from 'date-fns/locale';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { LanguageService } from '@service';
import { IconComponent } from '@components/app-icon/app-icon.component';

export const CUSTOM_DATE_FORMATS = {
  parse: {
    dateInput: 'dd/MM/yyyy',
    datetimeInput: 'dd/MM/yyyy HH:mm',
    timeInput: 'HH:mm',
    monthInput: 'MMMM',
    yearInput: 'yyyy',
  },
  display: {
    dateInput: 'dd/MM/yyyy',
    datetimeInput: 'dd/MM/yyyy HH:mm',
    timeInput: 'HH:mm',
    monthInput: 'MMMM/yyyy',
    yearInput: 'yyyy',
    monthYearLabel: 'yyyy MMMM',
    dateA11yLabel: 'LL',
    monthYearA11yLabel: 'MMMM yyyy',
    popupHeaderDateLabel: 'E, dd MMMM',
  },
};

@Component({
  selector: 'app-date-control',
  templateUrl: './date-control.component.html',
  standalone: true,
  imports: [
    FormsModule,
    MtxDatetimepickerModule,
    MtxDateFnsDatetimeModule,
    ReactiveFormsModule,
    ...MATERIAL_MODULE,
    IconComponent,
  ],
  providers: [
    {
      provide: DateAdapter,
      useClass: DateFnsAdapter,
      deps: [MAT_DATE_LOCALE],
    },
    {
      provide: DatetimeAdapter,
      useClass: DateFnsDateTimeAdapter,
    },
    {
      provide: MAT_DATE_LOCALE,
      useValue: vi,
    },

    { provide: MAT_DATE_FORMATS, useValue: CUSTOM_DATE_FORMATS },

    { provide: MTX_DATETIME_FORMATS, useValue: CUSTOM_DATE_FORMATS },
  ],
})
export class DateControlComponent
  extends FormGroupAbstractComponent
  implements OnInit
{
  get minDate(): Date | null {
    return this.item.minDate ? new Date(this.item.minDate) : null;
  }

  get maxDate(): Date | null {
    return this.item.maxDate ? new Date(this.item.maxDate) : null;
  }

  constructor(
    protected override readonly languageService: LanguageService,
    private readonly dateAdapter: DateAdapter<Date>
  ) {
    super(languageService);
  }

  ngOnInit(): void {
    this.setLanguage();
  }

  setLanguage(): void {
    this.dateAdapter.setLocale(vi);
  }

  override onChangeDate(event: any): void {
    const val = event.value;
    const isDateOnly = (this.item.dateType ?? 'date') === 'date';

    if (isDateOnly && val instanceof Date && !isNaN(val.getTime())) {
      const localStr = this.toLocalISOString(val);
      const ctrl = this.getControl();

      if (ctrl && ctrl.value !== localStr) {
        ctrl.patchValue(localStr, { emitEvent: false });
        ctrl.markAsDirty();
      }
    }

    this.onChange(val);
  }

  override onFocusOut(event: FocusEvent): void {
    this.controlFocusOut.emit(event);
  }

  protected getStartView(): MtxCalendarView {
    switch (this.item.type) {
      case 'date':
        return 'month';
      case 'month':
        return 'year';
      case 'year':
        return 'multi-year';
      default:
        return 'month';
    }
  }

  private toLocalISOString(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    const y = date.getFullYear();
    const m = pad(date.getMonth() + 1);
    const d = pad(date.getDate());
    return `${y}-${m}-${d}T00:00:00`;
  }
}
