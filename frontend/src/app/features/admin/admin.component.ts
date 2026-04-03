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
        name: 'Quản trị người dùng',
        icon: 'group',
        expanded: true,
        children: [
          {
            key: 'nguoi-dung',
            id: 'nguoi-dung',
            parentId: 'nguoi-dung-root',
            name: 'Quản lý tài khoản',
            icon: 'person',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'don-vi',
            id: 'don-vi',
            parentId: 'nguoi-dung-root',
            name: 'Quản lý đơn vị',
            icon: 'account_tree',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.DON_VI.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
      {
        key: 'cau-hinh-root',
        id: 'cau-hinh-root',
        name: 'Cấu hình hệ thống',
        icon: 'settings',
        expanded: true,
        children: [
          {
            key: 'vai-tro',
            id: 'vai-tro',
            parentId: 'cau-hinh-root',
            name: 'Quản lý vai trò',
            icon: 'manage_accounts',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.VAI_TRO.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'nam-hoc',
            id: 'nam-hoc',
            parentId: 'cau-hinh-root',
            name: 'Cấu hình năm học',
            icon: 'calendar_month',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.NAM_HOC.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'khoi',
            id: 'khoi',
            parentId: 'cau-hinh-root',
            name: 'Cấu hình khối',
            icon: 'dashboard',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.KHOI.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
      {
        key: 'hoc-tap-root',
        id: 'hoc-tap-root',
        name: 'Quản lý học tập',
        icon: 'school',
        expanded: true,
        children: [
          {
            key: 'lop',
            id: 'lop',
            parentId: 'hoc-tap-root',
            name: 'Quản lý lớp',
            icon: 'meeting_room',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.LOP.BASE_PATH}`,
          } as MenuItem,
          {
            key: 'mon-hoc',
            id: 'mon-hoc',
            parentId: 'hoc-tap-root',
            name: 'Quản lý môn học',
            icon: 'menu_book',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.MON_HOC.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
      {
        key: 'hoc-sinh-root',
        id: 'hoc-sinh-root',
        name: 'Học sinh',
        icon: 'groups',
        expanded: true,
        children: [
          {
            key: 'ho-so-hoc-sinh',
            id: 'ho-so-hoc-sinh',
            parentId: 'hoc-sinh-root',
            name: 'Hồ sơ học sinh',
            icon: 'badge',
            url: `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}/${NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH}`,
          } as MenuItem,
        ],
      } as MenuItem,
    ];
  }
}
