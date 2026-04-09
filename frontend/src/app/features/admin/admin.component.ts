/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Injector } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { takeUntil } from 'rxjs';

import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { NavigatorAction } from '@store/navigator';

import { MenuResponse } from '@app/model/admin/menu.model';
import { MenuService } from '@app/service/admin/menu.service';
import { MenuItem } from './admin.interface';

@Component({
  selector: 'admin',
  templateUrl: './admin.component.html',
  imports: [RouterOutlet],
})
export class AdminComponent extends ComponentBaseAbstract {
  constructor(
    protected override injector: Injector,
    private readonly menuService: MenuService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    queueMicrotask(() => this.loadDynamicMenu());
    this.menuService.menuChanged$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(() => this.loadDynamicMenu());
  }

  private loadDynamicMenu() {
    this.menuService.filter({}).subscribe({
      next: ({ data }) => {
        const dynamicMenu = this.buildMenuTree(data ?? []);

        this.store.dispatch(
          NavigatorAction.Update({
            newState: dynamicMenu as any,
          })
        );
      },
    });
  }

  private buildMenuTree(items: MenuResponse[]): MenuItem[] {
    const normalizedItems = items.map((item) => ({
      ...item,
      url: this.normalizeMenuUrl(item.code, item.url),
      icon: item.icon || this.getFallbackIcon(item.code),
    }));

    const itemMap = new Map<string | number, MenuItem>();

    normalizedItems.forEach((item) => {
      itemMap.set(item.id, {
        key: item.code,
        id: item.id,
        parentId: item.parentId,
        name: item.name,
        icon: item.icon ?? undefined,
        url: item.url ?? undefined,
        expanded: true,
        children: [],
      } as MenuItem);
    });

    const roots: MenuItem[] = [];

    normalizedItems.forEach((item) => {
      const node = itemMap.get(item.id)!;
      if (item.parentId != null && itemMap.has(item.parentId)) {
        itemMap.get(item.parentId)!.children!.push(node);
      } else {
        roots.push(node);
      }
    });

    return roots;
  }

  private normalizeMenuUrl(
    code: string,
    url?: string | null
  ): string | undefined {
    const adminBase = `/${NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH}`;
    const codeToUrl: Partial<Record<string, string>> = {
      ACCOUNT_MANAGEMENT: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH}`,
      UNIT_MANAGEMENT: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.DON_VI.BASE_PATH}`,
      ROLE_MANAGEMENT: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.VAI_TRO.BASE_PATH}`,
      FUNCTION_MANAGEMENT: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.MENU.BASE_PATH}`,
      SCHOOL_YEAR_CONFIG: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.NAM_HOC.BASE_PATH}`,
      GRADE_CONFIG: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.KHOI.BASE_PATH}`,
      CLASS_MANAGEMENT: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.LOP.BASE_PATH}`,
      SUBJECT_MANAGEMENT: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.MON_HOC.BASE_PATH}`,
      STUDENT_PROFILE: `${adminBase}/${NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH}`,
    };

    if (codeToUrl[code]) {
      return codeToUrl[code];
    }

    if (!url) return undefined;
    if (url.startsWith('/admin/')) {
      return `/Admin/${url.slice('/admin/'.length)}`;
    }
    if (url.startsWith('/Admin/')) {
      return url;
    }
    return url;
  }

  private getFallbackIcon(code: string): string {
    const codeToIcon: Record<string, string> = {
      USER_ADMIN: 'group',
      ACCOUNT_MANAGEMENT: 'person',
      UNIT_MANAGEMENT: 'account_tree',
      SYSTEM_CONFIG: 'settings',
      ROLE_MANAGEMENT: 'manage_accounts',
      FUNCTION_MANAGEMENT: 'account_tree',
      SCHOOL_YEAR_CONFIG: 'calendar_month',
      GRADE_CONFIG: 'dashboard',
      STUDY_MANAGEMENT: 'school',
      CLASS_MANAGEMENT: 'meeting_room',
      SUBJECT_MANAGEMENT: 'menu_book',
      STUDENT: 'groups',
      STUDENT_PROFILE: 'badge',
    };

    return codeToIcon[code] ?? 'menu';
  }
}
