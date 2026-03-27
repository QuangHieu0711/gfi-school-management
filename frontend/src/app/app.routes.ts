import { Routes } from '@angular/router';
import { AdminGuard } from '@guard';
import { LayoutComponent } from '@layout';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

export const routes: Routes = [
  {
    path: '',
    redirectTo: NAVIGATOR_ENDPOINT.LOGIN,
    pathMatch: 'full',
  },
  {
    path: NAVIGATOR_ENDPOINT.LOGIN,
    loadComponent: () =>
      import('@features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
    component: LayoutComponent,
    canActivate: [AdminGuard], // guarding token
    loadChildren: () =>
      import('@features/admin/admin.routes').then((m) => m.AdminRoutes),
  },

  {
    path: NAVIGATOR_ENDPOINT.ACCESS_DENIED,
    loadComponent: () => import('@layout').then((m) => m.AccessDeniedComponent),
  },
  {
    path: NAVIGATOR_ENDPOINT.SERVER_ERROR,
    loadComponent: () => import('@layout').then((m) => m.ServerErrorComponent),
  },
  {
    path: '**',
    loadComponent: () => import('@layout').then((m) => m.NotFoundComponent),
  },
];
