/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/consistent-type-definitions */
// import { Component, AfterViewInit } from '@angular/core';
// import { FormsModule, ReactiveFormsModule } from '@angular/forms';
// import { MtxSelectModule } from '@ng-matero/extensions/select';
// import { FormGroupAbstractComponent } from '@components/form-group';
// import { MATERIAL_MODULE } from '@modules';
// import { SVG_ICONS } from '@constant/icons';
// import { TranslateModule } from '@ngx-translate/core';
// import { IconComponent } from '@components/app-icon/app-icon.component';

// @Component({
//   selector: 'app-select-icon-control',
//   templateUrl: './select-icon-control.component.html',
//   standalone: true,
//   imports: [
//     FormsModule,
//     MtxSelectModule,
//     ReactiveFormsModule,
//     ...MATERIAL_MODULE,
//     TranslateModule,
//     IconComponent,
//   ],
// })
// export class SelectIconControlComponent
//   extends FormGroupAbstractComponent
//   implements AfterViewInit
// {
//   iconConstant = SVG_ICONS.map((name) => ({
//     value: `svg:${name}`,
//     label: name,
//   }));

//   override ngAfterViewInit(): void {
//     if (this.shouldAutoFocus())
//       setTimeout(() => this.getControl()?.markAsTouched(), 0);
//   }
// }

import { Component, AfterViewInit, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MtxSelectModule } from '@ng-matero/extensions/select';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { SVG_ICONS } from '@constant/icons';
import { TranslateModule } from '@ngx-translate/core';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { distinctUntilChanged } from 'rxjs/operators';

type IconOption = { value: string; label: string };

@Component({
  selector: 'app-select-icon-control',
  templateUrl: './select-icon-control.component.html',
  styleUrls: ['./select-icon-control.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    MtxSelectModule,
    ReactiveFormsModule,
    ...MATERIAL_MODULE,
    TranslateModule,
    IconComponent,
  ],
})
export class SelectIconControlComponent
  extends FormGroupAbstractComponent
  implements OnInit, AfterViewInit
{
  private readonly defaultIconOptions: IconOption[] = SVG_ICONS.map((name) => ({
    value: `svg:${name}`,
    label: name,
  }));

  get iconConstant(): IconOption[] {
    return this.item.options?.length
      ? (this.item.options as IconOption[])
      : this.defaultIconOptions;
  }

  ngOnInit(): void {
    const control = this.getControl();
    if (!control) return;

    const initValue = this.normalizeIconValue(control.value);
    if (initValue !== control.value) {
      control.setValue(initValue, { emitEvent: false });
    }

    control.valueChanges.pipe(distinctUntilChanged()).subscribe((value) => {
      const normalized = this.normalizeIconValue(value);
      if (normalized !== value) {
        control.setValue(normalized, { emitEvent: false });
      }
    });
  }

  override ngAfterViewInit(): void {
    if (this.shouldAutoFocus()) {
      setTimeout(() => this.getControl()?.markAsTouched(), 0);
    }
  }

  // ✅ mtx-select trackByFn thường chỉ nhận 1 tham số (item)
  trackByIconFn = (item: any) => {
    if (!item) return item;
    if (typeof item === 'string') return item;
    return item.value ?? item.label ?? item;
  };

  // ✅ luôn trả string để bind vào <app-icon [icon]>
  getIconValue(item: any): string {
    if (!item) return '';

    if (typeof item === 'object') {
      const raw = item.value ?? item.label ?? '';
      return this.normalizeIconValue(raw) ?? '';
    }

    return this.normalizeIconValue(item) ?? '';
  }

  getIconLabel(item: any): string {
    if (!item) return '';

    if (typeof item === 'object') {
      if (item.label) return String(item.label);

      const v = this.normalizeIconValue(item.value);
      return v ? v.replace(/^svg:/, '') : '';
    }

    const v = this.normalizeIconValue(item);
    return v ? v.replace(/^svg:/, '') : '';
  }

  private normalizeIconValue(value: unknown): string | null {
    if (value === null || value === undefined || value === '') return null;
    if (typeof value !== 'string') return null;

    const trimmed = value.trim();
    if (!trimmed) return null;

    if (this.iconConstant.some((option) => option.value === trimmed)) {
      return trimmed;
    }

    if (trimmed.startsWith('svg:')) return trimmed;

    const svgValue = `svg:${trimmed}`;
    if (this.iconConstant.some((option) => option.value === svgValue)) {
      return svgValue;
    }

    return trimmed;
  }
}
