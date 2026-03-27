import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ComponentBaseAbstract, LoadingIndicatorComponent } from '@layout';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  imports: [RouterOutlet, LoadingIndicatorComponent],
})
export class AppComponent extends ComponentBaseAbstract {
  protected override componentInit(): void {
    this.i18n.loadLanguage('common');
  }
}
