import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '@service';
import { catchError, map, of } from 'rxjs';
import { Store } from '@ngrx/store';
import { UserInfoAction } from '@store/user-info';
import { ICurrentUser, UserRole } from '@model/auth.model';
import { getObsValue } from '@utils/utils';
import { createResolvedUrl } from '@utils/resolved-path';

/**
 * A route guard that allows access only to authenticated users with the ADMIN role.
 * - Redirects unauthenticated users to LOGIN.
 * - Redirects authenticated non-admin users to BASE_PATH.
 * - If user info is not yet in the store, it fetches and caches it before checking roles.
 */
export const AdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const store = inject(Store);

  // Pre-resolve redirect paths
  const loginPath = createResolvedUrl(['LOGIN']);
  const basePath = createResolvedUrl(['BASE_PATH']);

  // Redirect unauthenticated users to the login page
  if (!authService.isAuthenticated()) return loginPath;

  // Attempt to get current user info from the store
  const currentUser = getObsValue(store.select((state) => state.userInfo));

  // If user info is already available, check if they are an admin
  if (currentUser.id) return isAdmin(currentUser) ? true : basePath;

  // If not available, fetch user info and update the store
  return authService.getCurrentUser().pipe(
    map((data) => {
      store.dispatch(UserInfoAction.Update({ newState: data }));
      return isAdmin(data) ? true : basePath;
    }),
    catchError(() => of(loginPath))
  );
};

/**
 * Utility function to check if the current user has the ADMIN role.
 */
const isAdmin = (currentUser: ICurrentUser): boolean => {
  return currentUser.role.name === UserRole.ADMIN;
};
