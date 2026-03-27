import { Routes } from '@angular/router';
import { AdminGuard, AuthGuard, NoAuthGuard } from '@guard';
import { LayoutComponent } from '@layout';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

export const routes: Routes = [
  {
    path: NAVIGATOR_ENDPOINT.BASE_PATH,
    component: LayoutComponent,
    canActivate: [AuthGuard], // guarding token
    loadChildren: () =>
      import('@features/map/map.routes').then((m) => m.MapRoutes),
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
    component: LayoutComponent,
    canActivate: [AdminGuard], // guarding token
    loadChildren: () =>
      import('@features/admin/admin.routes').then((m) => m.AdminRoutes),
  },
  {
    path: NAVIGATOR_ENDPOINT.BAO_CAO_DIA_CHAT.BAO_CAO_DIA_CHAT,
    component: LayoutComponent,
    canActivate: [AuthGuard], // guarding token
    loadChildren: () =>
      import('@features/bao-cao-dia-chat/bao-cao-dia-chat.routes').then(
        (m) => m.BaoCaoDiaChatRoutes
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.TAI_LIEU_NGUYEN_THUY.TAI_LIEU_NGUYEN_THUY,
    component: LayoutComponent,
    canActivate: [AuthGuard], // guarding token
    loadChildren: () =>
      import('@features/tai-lieu-nguyen-thuy/tai-lieu-nguyen-thuy.routes').then(
        (m) => m.TaiLieuNguyenThuyRoutes
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.LOGIN,
    canActivate: [NoAuthGuard], // guarding no token
    loadComponent: () =>
      import('@features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: NAVIGATOR_ENDPOINT.CHANGE_PASSWORD,
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('@features/change-password/change-password-dialog.component').then(
        (m) => m.ChangePasswordDialogComponent
      ),
  },
  {
    path: `${NAVIGATOR_ENDPOINT.FORGOT_PASSWORD}/verify`,
    canActivate: [NoAuthGuard], // guarding no token
    loadComponent: () =>
      import('@features/forgot-password/verify-code/verify-code.component').then(
        (m) => m.VerifyCodeComponent
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.FORGOT_PASSWORD,
    canActivate: [NoAuthGuard], // guarding no token
    loadComponent: () =>
      import('@features/forgot-password/request-reset/request-reset.component').then(
        (m) => m.RequestResetComponent
      ),
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
    path: NAVIGATOR_ENDPOINT.DE_AN_PHUONG_AN.BASE_PATH,
    canActivate: [AuthGuard],
    component: LayoutComponent,
    loadChildren: () =>
      import('@features/de-an-phuong-an/de-an-phuong-an.routes').then(
        (r) => r.DeAnPhuongAnRoutes
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.QUAN_TRI_TAI_NGUYEN.BASE_PATH,
    canActivate: [AuthGuard],
    component: LayoutComponent,
    loadChildren: () =>
      import('@features/quan-tri-tai-nguyen/quan-tri-tai-nguyen.routes').then(
        (r) => r.QuanTriTaiNguyenRoutes
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.DU_AN_KHAI_THAC.BASE_PATH,
    canActivate: [AuthGuard],
    component: LayoutComponent,
    loadChildren: () =>
      import('@features/du-an-khai-thac/du-an-khai-thac.routes').then(
        (r) => r.DuAnKhaiThacRoutes
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.DONG_CUA_MO.BASE_PATH,
    canActivate: [AuthGuard],
    component: LayoutComponent,
    loadChildren: () =>
      import('@features/dong-cua-mo/dong-cua-mo.routes').then(
        (r) => r.DongCuaMoRoutes
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.MAP.BASE_PATH,
    loadChildren: () =>
      import('@features/map/map.routes').then((r) => r.MapRoutes),
  },
  {
    path: '**',
    loadComponent: () => import('@layout').then((m) => m.NotFoundComponent),
  },
];
