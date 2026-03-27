import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MtxCheckboxGroupModule } from '@ng-matero/extensions/checkbox-group';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-checkbox-control',
  templateUrl: './checkbox-control.component.html',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, MtxCheckboxGroupModule, ...MATERIAL_MODULE],
})
export class CheckboxControlComponent extends FormGroupAbstractComponent {}
