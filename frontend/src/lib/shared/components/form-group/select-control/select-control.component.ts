import { Component, AfterViewInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MtxSelectModule } from '@ng-matero/extensions/select';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-select-control',
  templateUrl: './select-control.component.html',
  standalone: true,
  imports: [
    FormsModule,
    MtxSelectModule,
    ReactiveFormsModule,
    ...MATERIAL_MODULE,
    TranslateModule,
    CommonModule,
  ],
})
export class SelectControlComponent
  extends FormGroupAbstractComponent
  implements AfterViewInit
{
  override ngAfterViewInit(): void {
    if (this.shouldAutoFocus())
      setTimeout(() => this.getControl()?.markAsTouched(), 0);
  }

  getSelectedLabel(): string | null {
    const selected = this.item.options?.find(
      (opt) => opt.value === this.getControlValue()
    );
    return selected?.label ?? null;
  }

  get appendToTarget(): string {
    if (this.item.appendToBody) return 'body';
    return this.isInsideDialog ? '.mat-mdc-dialog-panel' : '.content-wrapper';
  }
}
