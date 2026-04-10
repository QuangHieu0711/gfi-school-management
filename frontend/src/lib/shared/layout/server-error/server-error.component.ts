import { Component, Injector } from '@angular/core';
import { RouterModule } from '@angular/router';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'server-error-component',
  standalone: true,
  templateUrl: './server-error.component.html',
  imports: [...MATERIAL_MODULE, RouterModule, TranslateModule, IconComponent],
})
export class ServerErrorComponent extends ComponentBaseAbstract {
  constructor(protected override injector: Injector) {
    super(injector);
  }
}
