import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { NavigatorAction } from '@store/navigator';

import { MenuItem } from './admin.interface';

@Component({
  selector: 'admin',
  templateUrl: './admin.component.html',
  imports: [RouterOutlet],
})
export class AdminComponent extends ComponentBaseAbstract {
  protected override componentInit(): void {
    queueMicrotask(() => this.loadDynamicMenu());
  }

  private loadDynamicMenu() {
    const dynamicMenu = this.buildMenuTree();

    this.store.dispatch(
      NavigatorAction.Update({
        newState: dynamicMenu as any,
      })
    );
  }

  private buildMenuTree(): MenuItem[] {
    return [
      {
        key: 'nguoi-dung-root',
        id: 'nguoi-dung-root',
        name: 'Qu\u1ea3n tr\u1ecb ng\u01b0\u1eddi d\u00f9ng',
        icon: 'group',
        expanded: true,
        children: [
          {
            key: 'nguoi-dung',
            id: 'nguoi-dung',
            parentId: 'nguoi-dung-root',
            name: 'Qu\u1ea3n l\u00fd t\u00e0i kho\u1ea3n',
            icon: 'person',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'don-vi',
            id: 'don-vi',
            parentId: 'nguoi-dung-root',
            name: 'Qu\u1ea3n l\u00fd \u0111\u01a1n v\u1ecb',
            icon: 'account_tree',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.DON_VI.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
      {
        key: 'cau-hinh-root',
        id: 'cau-hinh-root',
        name: 'C\u1ea5u h\u00ecnh h\u1ec7 th\u1ed1ng',
        icon: 'settings',
        expanded: true,
        children: [
          {
            key: 'vai-tro',
            id: 'vai-tro',
            parentId: 'cau-hinh-root',
            name: 'Qu\u1ea3n l\u00fd vai tr\u00f2',
            icon: 'manage_accounts',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.VAI_TRO.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'nam-hoc',
            id: 'nam-hoc',
            parentId: 'cau-hinh-root',
            name: 'C\u1ea5u h\u00ecnh n\u0103m h\u1ecdc',
            icon: 'calendar_month',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.NAM_HOC.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'khoi',
            id: 'khoi',
            parentId: 'cau-hinh-root',
            name: 'C\u1ea5u h\u00ecnh kh\u1ed1i',
            icon: 'dashboard',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.KHOI.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
      {
        key: 'hoc-tap-root',
        id: 'hoc-tap-root',
        name: 'Qu\u1ea3n l\u00fd h\u1ecdc t\u1eadp',
        icon: 'school',
        expanded: true,
        children: [
          {
            key: 'lop',
            id: 'lop',
            parentId: 'hoc-tap-root',
            name: 'Qu\u1ea3n l\u00fd l\u1edbp',
            icon: 'meeting_room',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.LOP.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'mon-hoc',
            id: 'mon-hoc',
            parentId: 'hoc-tap-root',
            name: 'Qu\u1ea3n l\u00fd m\u00f4n h\u1ecdc',
            icon: 'menu_book',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.MON_HOC.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
    ];
  }
}
