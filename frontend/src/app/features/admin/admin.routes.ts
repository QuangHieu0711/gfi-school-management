import { Routes } from '@angular/router';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
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
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/vai-tro/vai-tro.component').then(
            (m) => m.VaiTroComponent
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
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/lop/lop.component').then((m) => m.LopComponent),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.MON_HOC.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
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
];
