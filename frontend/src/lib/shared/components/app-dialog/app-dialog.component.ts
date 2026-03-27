import { Component, Input, TemplateRef } from '@angular/core';
import { MATERIAL_MODULE } from '@modules';
import { ComponentBaseAbstract } from '@layout';
import { CommonModule } from '@angular/common';
import { ID_TYPE } from '@model/response.model';

@Component({
  selector: 'app-dialog-container',
  templateUrl: './app-dialog.component.html',
  imports: [CommonModule, ...MATERIAL_MODULE],
  standalone: true,
})
export class AppDialogComponent extends ComponentBaseAbstract {
  @Input() dialogTitle!: string;
  @Input() dialogTitleCode!: ID_TYPE;
  @Input() dialogActionTemplate!: TemplateRef<unknown>;
  @Input() dialogTitleClass?: string;
}
