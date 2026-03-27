import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '@service';
import { createResolvedUrl } from '@utils/resolved-path';

/**
 * A route guard that prevents authenticated users from accessing routes meant for unauthenticated users
 * (e.g., login or register pages). If the user is already authenticated, redirect them to the BASE_PATH.
 */
export const NoAuthGuard: CanActivateFn = () => {
  const authService = inject(AuthService);

  // If the user is authenticated, redirect them away from the route (e.g., to dashboard)
  if (authService.isAuthenticated()) {
    if (authService.mustChangePassword) {
      return createResolvedUrl(['CHANGE_PASSWORD']);
    }

    return createResolvedUrl(['BASE_PATH']);
  }

  return true;
};
