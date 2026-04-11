import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { AuthService, PermissionCheckService } from '@service';

export const PermissionGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot
) => {
  const permissionCheckService = inject(PermissionCheckService);
  const authService = inject(AuthService);
  const router = inject(Router);
  const menuCode = String(route.data?.['menuCode'] ?? '').trim();

  if (!menuCode) {
    return true;
  }

  if (permissionCheckService.canView(menuCode)) {
    return true;
  }

  const fallbackRule = authService.currentUser?.role?.rules?.find(
    (rule) =>
      (rule.menuCode ?? '').trim().toUpperCase() === menuCode.toUpperCase()
  );

  if (Number(fallbackRule?.isView ?? 0) === 1) {
    return true;
  }

  const flattenMenus = <T extends { children?: T[] }>(menus: T[]): T[] =>
    (menus ?? []).flatMap((menu) => [
      menu,
      ...flattenMenus(menu.children ?? []),
    ]);

  const fallbackMenu = flattenMenus(
    authService.currentUser?.permissions?.menus ?? []
  ).find(
    (menu) =>
      (menu.menuCode ?? '').trim().toUpperCase() === menuCode.toUpperCase()
  );

  if (Number(fallbackMenu?.actions?.isView ?? 0) === 1) {
    return true;
  }

  return router.createUrlTree([NAVIGATOR_ENDPOINT.ACCESS_DENIED]);
};
