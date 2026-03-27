import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'access-denied-component',
  templateUrl: './access-denied.component.html',
  imports: [...MATERIAL_MODULE, RouterModule, TranslateModule, IconComponent],
})
export class AccessDeniedComponent extends ComponentBaseAbstract {}
