import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '@service';
import { createResolvedUrl } from '@utils/resolved-path';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

/**
 * A route guard that prevents authenticated users from accessing routes meant for unauthenticated users
 * (e.g., login or register pages). If the user is already authenticated, redirect them to the BASE_PATH.
 */
export const NoAuthGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If the user is authenticated, redirect them away from the route (e.g., to dashboard)
  if (authService.isAuthenticated()) {
    if (authService.mustChangePassword) {
      return createResolvedUrl(['CHANGE_PASSWORD']);
    }

    return router.parseUrl(
      `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH}`
    );
  }

  return true;
};
