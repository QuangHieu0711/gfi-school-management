import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FormGroupAbstractComponent } from '@components/form-group';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-radio-control',
  templateUrl: './radio-control.component.html',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, ...MATERIAL_MODULE],
})
export class RadioControlComponent extends FormGroupAbstractComponent {}
