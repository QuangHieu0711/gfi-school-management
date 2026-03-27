import { Component, OnInit } from '@angular/core';
import {
  FormsModule,
  ReactiveFormsModule,
  AbstractControl,
} from '@angular/forms';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { MAT_DATE_FORMATS } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { LanguageService } from '@service';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-date-range-control',
  templateUrl: './date-range-control.component.html',
  styles: [
    `
      :host ::ng-deep .mat-date-range-input-separator {
        margin-left: 2px;
        margin-right: 6px;
        padding: 0;
      }
    `,
  ],
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    ...MATERIAL_MODULE,
    IconComponent,
    TranslateModule,
  ],
  providers: [
    {
      provide: MAT_DATE_LOCALE,
      useValue: 'vi-VN',
    },
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: {
          dateInput: 'DD/MM/YYYY',
        },
        display: {
          dateInput: 'DD/MM/YYYY',
          monthYearLabel: 'MMM YYYY',
          dateA11yLabel: 'LL',
          monthYearA11yLabel: 'MMMM YYYY',
        },
      },
    },
  ],
})
export class DateRangeControlComponent
  extends FormGroupAbstractComponent
  implements OnInit
{
  startDateKey = '';
  endDateKey = '';
  rangeSeparator = ' - ';
  startPlaceholder = '';
  endPlaceholder = '';

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
    const keys = this.item.key.split('-');
    this.startDateKey = keys[0] || 'startDate';
    this.endDateKey = keys[1] || 'endDate';
    this.initPlaceholders();
  }

  private initPlaceholders(): void {
    const customStart = String(this.item?.startPlaceholder ?? '').trim();
    const customEnd = String(this.item?.endPlaceholder ?? '').trim();
    if (customStart || customEnd) {
      this.startPlaceholder = customStart || this.startPlaceholder;
      this.endPlaceholder = customEnd || this.endPlaceholder;
      return;
    }

    const raw = String(this.item?.placeholder ?? '').trim();
    if (!raw) return;

    const [start, end] = raw.split(/\s-\s|-/).map((x) => x.trim());
    this.startPlaceholder = start || this.startPlaceholder;
    this.endPlaceholder = end || this.endPlaceholder;
  }

  setLanguage(): void {
    this.dateAdapter.setLocale('vi-VN');
  }

  /**
   * Override getControl to return the start date control for date range
   * Since date range has two controls (start and end), we return the start one
   */
  override getControl(): AbstractControl | null {
    const startControl = this.f?.[this.startDateKey];
    if (!startControl) {
      console.warn(`Control not found: ${this.startDateKey}`);
      return null;
    }
    return startControl;
  }

  /**
   * Override isInvalidControl to check both start and end date controls
   */
  override isInvalidControl(errorKey?: string): boolean {
    const startControl = this.f?.[this.startDateKey];
    const endControl = this.f?.[this.endDateKey];

    if (!startControl && !endControl) return false;

    return (
      (startControl?.touched || endControl?.touched) &&
      (errorKey
        ? startControl?.hasError(errorKey) || endControl?.hasError(errorKey)
        : startControl?.invalid || endControl?.invalid)
    );
  }

  override onFocusOut(event: FocusEvent): void {
    this.controlFocusOut.emit(event);
  }
}
