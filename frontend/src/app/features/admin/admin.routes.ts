import { Routes } from '@angular/router';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
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
    path: NAVIGATOR_ENDPOINT.ADMIN.CHUC_NANG.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/chuc-nang/chuc-nang.component').then(
            (m) => m.ChucNangComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.NHOM_QUYEN.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/nhom-quyen/nhom-quyen.component').then(
            (m) => m.NhomQuyenComponent
          ),
      },
      {
        path: PATH.TAO_MOI,
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/nhom-quyen/them-nhom-quyen/them-nhom-quyen.component').then(
            (m) => m.ThemNhomQuyenComponent
          ),
      },
      {
        path: `${PATH.CAP_NHAT}/:roleId`,
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/nhom-quyen/them-nhom-quyen/them-nhom-quyen.component').then(
            (m) => m.ThemNhomQuyenComponent
          ),
      },
      {
        path: `${PATH.CHI_TIET}/:roleId`,
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/nhom-quyen/them-nhom-quyen/them-nhom-quyen.component').then(
            (m) => m.ThemNhomQuyenComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_VUNG_MO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-vung-mo/danh-muc-vung-mo.component').then(
            (m) => m.DanhMucVungMoComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_KHU_MO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-khu-mo/danh-muc-khu-mo.component').then(
            (m) => m.DanhMucKhuMoComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_BAO_CAO_TAI_NGUYEN.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-bao-cao-tai-nguyen/danh-muc-bao-cao-tai-nguyen.component').then(
            (m) => m.DanhMucBaoCaoTaiNguyenComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_BAO_CAO_DIA_CHAT.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-bao-cao-dia-chat/danh-muc-bao-cao-dia-chat.component').then(
            (m) => m.DanhMucBaoCaoDiaChatComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_KHOANG_SAN.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-khoang-san/danh-muc-khoang-san.component').then(
            (m) => m.DanhMucKhoangSanComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_DE_AN_DONG_CUA_MO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-dong-cua-mo/danh-muc-dong-cua-mo.component').then(
            (m) => m.DanhMucDongCuaMoComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_HANG_MUC.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-hang-muc-thanh-toan/danh-muc-hang-muc-thanh-toan.component').then(
            (m) => m.DanhMucHangMucThanhToanComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DU_AN_KHAI_THAC.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-du-an-khai-thac/danh-muc-du-an-khai-thac.component').then(
            (m) => m.DanhMucDuAnKhaiThacComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_TAI_LIEU_NGUYEN_THUY.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-tai-lieu-nguyen-thuy/danh-muc-tai-lieu-nguyen-thuy.component').then(
            (m) => m.DanhMucTaiLieuNguyenThuyComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_MUC_DE_AN_THAM_DO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-muc-de-an-tham-do/danh-muc-de-an-tham-do.component').then(
            (m) => m.DanhMucDeAnThamDoComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_SACH_BAN_DO.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-sach-ban-do/danh-sach-ban-do.component').then(
            (m) => m.DanhSachBanDoComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_SACH_UNG_DUNG.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-sach-ung-dung/danh-sach-ung-dung.component').then(
            (m) => m.Danhsachungdung
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.DANH_SACH_UNG_DUNG.EDIT,
    component: AdminComponent,
    children: [
      {
        path: ':id',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/danh-sach-ung-dung/chinh-sua-danh-sach-ung-dung/chinh-sua-danh-sach-ung-dung.component').then(
            (m) => m.Chinhsuadanhsachungdung
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.SAO_LUU_DU_LIEU.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/sao-luu-phuc-hoi-du-lieu/sao-luu.component').then(
            (m) => m.SaoLuuComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.LICH_SU_TRUY_CAP_NGUOI_DUNG.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/lich-su-truy-cap-nguoi-dung/lich-su-truy-cap-nguoi-dung.component').then(
            (m) => m.LichSuTruyCapNguoiDungComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.CANH_BAO_DAPA.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/canh-bao-dapa/canh-bao-dapa.component').then(
            (m) => m.CanhBaoDAPAComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.CANH_BAO_QTTN.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/canh-bao-qttn/canh-bao-qttn.component').then(
            (m) => m.CanhBaoQTTNComponent
          ),
      },
    ],
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.TIEN_DO_THANH_TOAN.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/tien-do-thanh-toan/tien-do-thanh-toan.component').then(
            (m) => m.TienDoThanhToanComponent
          ),
      },
    ],
  },
  {
    path: PATH.TAO_MOI,
    providers: [
      provideState({ name: 'style', reducer: StyleReducer }),
      provideEffects(StyleEffect),
    ],
    loadComponent: () =>
      import('@features/admin/tien-do-thanh-toan/them-canh-bao/them-canh-bao.component').then(
        (m) => m.ThemCanhBaoComponent
      ),
  },
  {
    path: NAVIGATOR_ENDPOINT.ADMIN.TIEN_DO_THI_CONG.BASE_PATH,
    component: AdminComponent,
    children: [
      {
        path: '',
        providers: [
          provideState({ name: 'style', reducer: StyleReducer }),
          provideEffects(StyleEffect),
        ],
        loadComponent: () =>
          import('@features/admin/tien-do-thi-cong/tien-do-thi-cong.component').then(
            (m) => m.TienDoThiCongComponent
          ),
      },
    ],
  },
  {
    path: PATH.TAO_MOI,
    providers: [
      provideState({ name: 'style', reducer: StyleReducer }),
      provideEffects(StyleEffect),
    ],
    loadComponent: () =>
      import('@features/admin/tien-do-thi-cong/them-canh-bao/them-canh-bao.component').then(
        (m) => m.ThemCanhBaoComponent
      ),
  },
];
