import {
  ApplicationConfig,
  importProvidersFrom,
  inject,
  provideAppInitializer,
  provideZoneChangeDetection,
  isDevMode,
} from '@angular/core';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
// library
import { provideStore } from '@ngrx/store';
import { ToastrModule } from 'ngx-toastr';
import { TranslateModule } from '@ngx-translate/core';
import { MatIconRegistry } from '@angular/material/icon';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideNativeDatetimeAdapter } from '@ng-matero/extensions/core';
// services
import {
  ApiErrorInterceptor,
  AuthInterceptor,
  StripFetchOptionsInterceptor,
} from '@interceptors';
import { routes } from './app.routes';
import { provideEffects } from '@ngrx/effects';
import { reducers } from '@store/reducers';
import { effects } from '@store/effects';
import { FALLBACK_LANGUAGE } from '@constant/constant';
import { provideDateFnsDatetimeAdapter } from '@ng-matero/extensions-date-fns-adapter';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAppInitializer(() => {
      const initializerFn = ((iconRegistry: MatIconRegistry) => () => {
        const defaultFontSetClasses = iconRegistry.getDefaultFontSetClass();
        const outlinedFontSetClasses = defaultFontSetClasses
          .filter((fontSetClass) => fontSetClass !== 'material-icons')
          .concat(['material-symbols-outlined']);
        iconRegistry.setDefaultFontSetClass(...outlinedFontSetClasses);
      })(inject(MatIconRegistry));
      return initializerFn();
    }),
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Implement routing
    provideRouter(routes),
    // Implement store
    provideStore(reducers),
    provideEffects(effects),
    // Implement interceptor
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ApiErrorInterceptor,
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: StripFetchOptionsInterceptor,
      multi: true,
    },
    // Implement ngrx toastr
    provideAnimations(),
    importProvidersFrom([
      ToastrModule.forRoot(),
      TranslateModule.forRoot({
        defaultLanguage: FALLBACK_LANGUAGE,
      }),
    ]),
    // Implement mtx extensions
    provideNativeDateAdapter(), // Material's DateAdapter (dates)
    provideNativeDatetimeAdapter(), // Ng-Matero's DatetimeAdapter (date+time)
    provideDateFnsDatetimeAdapter(),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() }),
    MessageService,
    ConfirmationService,
    providePrimeNG({
      theme: {
        preset: Aura
      }
    })

  ],
};
