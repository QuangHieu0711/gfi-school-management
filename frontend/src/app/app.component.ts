import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoadingIndicatorComponent } from '@layout';
import { LanguageService } from '@service';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  imports: [RouterOutlet, LoadingIndicatorComponent],
})
export class AppComponent implements OnInit {
  private readonly i18n = inject(LanguageService);

  ngOnInit(): void {
    this.i18n.loadLanguage('common');
  }
}
