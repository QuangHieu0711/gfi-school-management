import { ISidebarItem } from '@model/menu.model';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

export const MENU: ISidebarItem[] = [
  {
    key: 'admin',
    label: 'Quản lý người dùng',
    icon: 'svg:user-group',
    url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH}`,
  },
];
