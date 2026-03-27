import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { TranslateModule } from '@ngx-translate/core';
import { IconComponent } from '@components/app-icon/app-icon.component';

@Component({
  selector: 'not-found-component',
  templateUrl: './not-found.component.html',
  imports: [...MATERIAL_MODULE, RouterModule, TranslateModule, IconComponent],
})
export class NotFoundComponent extends ComponentBaseAbstract {}
