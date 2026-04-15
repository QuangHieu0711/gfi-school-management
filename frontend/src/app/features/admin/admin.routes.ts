import { Routes } from '@angular/router';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import { PermissionGuard } from '@guard';
import { AdminComponent } from '@features/admin/admin.component';

import { provideState } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';

import { StyleReducer } from '@store/style';
import { StyleEffect } from '@store/style/effect';

export const AdminRoutes: Routes = [
  {
    path: '',
    providers: [
      provideState({ name: 'style', reducer: StyleReducer }),
      provideEffects(StyleEffect),
    ],
    redirectTo: NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH,
    pathMatch: 'full',
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'ACCOUNT_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/nguoi-dung/nguoi-dung.component').then(
            (m) => m.NguoiDungComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DON_VI.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'UNIT_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/don-vi/don-vi.component').then(
            (m) => m.DonViComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.VAI_TRO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'ROLE_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/vai-tro/vai-tro.component').then(
            (m) => m.VaiTroComponent
          ),
      },
      {
        path: 'cau-hinh/:id',
        canActivate: [PermissionGuard],
        data: { menuCode: 'ROLE_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('./vai-tro/cau-hinh-vai-tro/cau-hinh-vai-tro.component').then(
            (m) => m.CauHinhVaiTroComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.MENU.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'FUNCTION_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/menu/menu.component').then(
            (m) => m.MenuComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.NAM_HOC.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'SCHOOL_YEAR_CONFIG' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/nam-hoc/nam-hoc.component').then(
            (m) => m.NamHocComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.KHOI.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'GRADE_CONFIG' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/khoi/khoi.component').then(
            (m) => m.KhoiComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.LOP.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'CLASS_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/lop/lop.component').then(
            (m) => m.LopComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.MON_HOC.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'SUBJECT_MANAGEMENT' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/mon-hoc/mon-hoc.component').then(
            (m) => m.MonHocComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'STUDENT_PROFILE' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('./hoc-sinh/hoc-sinh.component').then(
            (m) => m.HocSinhComponent
          ),
      },
      {
        path: `${PATH.CHI_TIET}/:id`,
        canActivate: [PermissionGuard],
        data: { menuCode: 'STUDENT_PROFILE' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('./hoc-sinh/ho-so-hoc-sinh/chi-tiet/chi-tiet-hoc-sinh.component').then(
            (m) => m.ChiTietHocSinhComponent
          ),
      },
      {
        path: PATH.TAO_MOI,
        canActivate: [PermissionGuard],
        data: { menuCode: 'STUDENT_PROFILE' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('./hoc-sinh/ho-so-hoc-sinh/tao-moi/tao-moi-hoc-sinh.component').then(
            (m) => m.TaoMoiHocSinhComponent
          ),
      },
      {
        path: `${PATH.CAP_NHAT}/:id`,
        canActivate: [PermissionGuard],
        data: { menuCode: 'STUDENT_PROFILE' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('./hoc-sinh/ho-so-hoc-sinh/tao-moi/tao-moi-hoc-sinh.component').then(
            (m) => m.TaoMoiHocSinhComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        canActivate: [PermissionGuard],
        data: { menuCode: 'STAFF_PROFILE' },
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('./can-bo/can-bo.component').then((m) => m.CanBoComponent),
      },
    ],
  },
];
