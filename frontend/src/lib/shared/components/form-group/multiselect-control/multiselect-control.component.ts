import { CommonModule } from '@angular/common';
import { Component, AfterViewInit, ChangeDetectorRef, inject, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { MtxSelect, MtxSelectModule } from '@ng-matero/extensions/select';
import { FormGroupAbstractComponent } from '@components/form-group';
import { IOptions } from '@model/form-control.model';
import { MATERIAL_MODULE } from '@modules';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-multiselect-control',
  templateUrl: './multiselect-control.component.html',
  standalone: true,
  imports: [CommonModule, FormsModule, MtxSelectModule, ReactiveFormsModule, ...MATERIAL_MODULE, TranslateModule],
})
export class MultipleSelectControlComponent extends FormGroupAbstractComponent implements AfterViewInit {
  @ViewChild('multiselect', { static: true }) multiselect!: MtxSelect;
  cdref = inject(ChangeDetectorRef);
  override ngAfterViewInit(): void {
    if (this.shouldAutoFocus()) setTimeout(() => this.getControl()?.markAsTouched(), 0);
  }

  getItemLabel(item: unknown): string {
    return (item as IOptions).label;
  }

  onClear(): void {
    this.setControlValue([]);
    this.adjustPosition();
  }

  isSelectAll(): boolean {
    const value = this.getControlValue();
    const enabledOptions = this.item.options?.filter((item) => !item.disabled) ?? [];
    return (
      Array.isArray(value) &&
      value.length === enabledOptions.length &&
      value.every((v) => enabledOptions.some((option) => option.value === v))
    );
  }
  toggleSelectAll($event: MatCheckboxChange): void {
    let value: string[] = [];
    if ($event.checked) value = this.item.options?.filter((item) => !item.disabled).map((item) => item.value) as string[];
    this.setControlValue(value);
    this.adjustPosition();
  }

  adjustPosition(): void {
    this.multiselect.focus();
    setTimeout(() => {
      // Trong Angular 20, ngSelect là Signal => cần gọi như hàm
      const ngSelectInstance = (this.multiselect.ngSelect as any)?.();
      ngSelectInstance?.dropdownPanel?.adjustPosition?.();
      this.cdref.detectChanges();
    }, 0);
  }
}
