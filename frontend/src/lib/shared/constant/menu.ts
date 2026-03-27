import { ISidebarItem } from '@model/menu.model';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

export const MENU: ISidebarItem[] = [
  {
    key: 'dashboard',
    label: 'Trang chủ',
    icon: 'home',
    url: '/',
  },
  {
    key: 'taiLieuNguyenThuy',
    label: 'Tài liệu nguyên thủy',
    icon: 'svg:tailieunguyenthuy_menu',
    url: '/' + NAVIGATOR_ENDPOINT.TAI_LIEU_NGUYEN_THUY.TAI_LIEU_NGUYEN_THUY,
  },
  {
    key: 'baoCaoDiaChat',
    label: 'Báo cáo địa chất',
    icon: 'svg:baocaodiachat_menu',
    url: '/bao-cao-dia-chat',
  },
  {
    key: 'deAnPhuongAn',
    label: 'Đề án phương án',
    icon: 'svg:deanphuongan_menu',
    url: '/' + NAVIGATOR_ENDPOINT.DE_AN_PHUONG_AN.BASE_PATH,
  },
  {
    key: 'quanTriTaiNguyen',
    label: 'Quản trị tài nguyên',
    icon: 'svg:quantritainguyen_menu',
    url:
      '/' +
      NAVIGATOR_ENDPOINT.QUAN_TRI_TAI_NGUYEN.BASE_PATH +
      '/' +
      NAVIGATOR_ENDPOINT.QUAN_TRI_TAI_NGUYEN.BAO_CAO,
  },
  {
    key: 'duAnKhaiThac',
    label: 'Dự án khai thác',
    icon: 'svg:data-mining',
    url: '/' + NAVIGATOR_ENDPOINT.DU_AN_KHAI_THAC.BASE_PATH,
  },
  {
    key: 'dongCuaMo',
    label: 'Đề án đóng cửa mỏ',
    icon: 'svg:dongcuamo_menu',
    url: '/' + NAVIGATOR_ENDPOINT.DONG_CUA_MO.BASE_PATH,
  },
  {
    key: 'admin',
    label: 'Quản trị hệ thống',
    icon: 'svg:user-group',
    url: '/' + NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
  },
];
