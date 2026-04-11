import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoadingIndicatorComponent } from '@layout';
import { LanguageService, AuthService } from '@service';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  imports: [RouterOutlet, LoadingIndicatorComponent],
})
export class AppComponent implements OnInit {
  private readonly i18n = inject(LanguageService);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    // Restore session from storage (tokens, user info, permissions)
    this.authService.restoreStoredSession();
    // Load language
    this.i18n.loadLanguage('common');
  }
}
