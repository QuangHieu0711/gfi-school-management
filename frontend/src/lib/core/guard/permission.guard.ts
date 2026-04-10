import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { PermissionCheckService } from '@service';

export const PermissionGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot
) => {
  const permissionCheckService = inject(PermissionCheckService);
  const router = inject(Router);
  const menuCode = String(route.data?.['menuCode'] ?? '').trim();

  if (!menuCode) {
    return true;
  }

  if (permissionCheckService.canView(menuCode)) {
    return true;
  }

  return router.createUrlTree([NAVIGATOR_ENDPOINT.ACCESS_DENIED]);
};
