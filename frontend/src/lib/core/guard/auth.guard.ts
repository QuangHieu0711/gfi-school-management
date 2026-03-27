import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '@service';
import { createResolvedUrl } from '@utils/resolved-path';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

/**
 * A route guard function that restricts access to authenticated users only.
 * If the user is not authenticated, they will be redirected to the LOGIN route.
 */
export const AuthGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);

  // If the user is not authenticated, redirect them to the resolved LOGIN URL
  if (!authService.isAuthenticated()) return createResolvedUrl(['LOGIN']);

  const isChangePasswordPage = state.url.startsWith(
    `/${NAVIGATOR_ENDPOINT.CHANGE_PASSWORD}`
  );

  if (authService.mustChangePassword && !isChangePasswordPage) {
    return createResolvedUrl(['CHANGE_PASSWORD']);
  }

  return true;
};
