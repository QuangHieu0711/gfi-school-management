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
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
            key: 'cau-hinh-root',
            id: 'cau-hinh-root',
            parentId: 'nguoi-dung-root',
            name: 'Cấu hình',
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
            ],
          } as MenuItem,
        ],
      } as MenuItem,
    ];
  }
}
