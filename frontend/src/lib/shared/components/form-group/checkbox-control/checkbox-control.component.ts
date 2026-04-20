import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxChange } from '@angular/material/checkbox';
import { MtxCheckboxGroupModule } from '@ng-matero/extensions/checkbox-group';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-checkbox-control',
  templateUrl: './checkbox-control.component.html',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, MtxCheckboxGroupModule, ...MATERIAL_MODULE],
})
export class CheckboxControlComponent extends FormGroupAbstractComponent {
  get hasOptions(): boolean {
    return Array.isArray(this.item?.options) && this.item.options.length > 0;
  }

  onCheckboxChange(event: MatCheckboxChange): void {
    this.emitValueChanged(event);
  }
}
