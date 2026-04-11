/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Injector } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { takeUntil } from 'rxjs';

import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { ComponentBaseAbstract } from '@layout';
import { NavigatorAction } from '@store/navigator';
import { PermissionService } from '@service';

import { MenuItem } from './admin.interface';

@Component({
  selector: 'admin',
  templateUrl: './admin.component.html',
  imports: [RouterOutlet],
})
export class AdminComponent extends ComponentBaseAbstract {
  constructor(
    protected override injector: Injector,
    private readonly permissionService: PermissionService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    queueMicrotask(() => this.loadMenuFromPermissions());
    this.permissionService.rules$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(() => this.loadMenuFromPermissions());
  }

  private loadMenuFromPermissions() {
    const rules = this.permissionService.rules;
    const menu = this.buildMenuFromRules(rules);
    this.store.dispatch(
      NavigatorAction.Update({
        newState: menu as any,
      })
    );
  }

  private buildMenuFromRules(rules: any[]): MenuItem[] {
    const menuConfigMap: Record<string, { name: string; ordinal: number }> = {
      ACCOUNT_MANAGEMENT: { name: 'Quản lý người dùng', ordinal: 1 },
      UNIT_MANAGEMENT: { name: 'Quản lý đơn vị', ordinal: 2 },
      ROLE_MANAGEMENT: { name: 'Quản lý vai trò', ordinal: 3 },
      FUNCTION_MANAGEMENT: { name: 'Quản lý menu', ordinal: 4 },
      SCHOOL_YEAR_CONFIG: { name: 'Quản lý năm học', ordinal: 5 },
      GRADE_CONFIG: { name: 'Quản lý khối', ordinal: 6 },
      CLASS_MANAGEMENT: { name: 'Quản lý lớp', ordinal: 7 },
      SUBJECT_MANAGEMENT: { name: 'Quản lý môn học', ordinal: 8 },
      STUDENT_PROFILE: { name: 'Quản lý học sinh', ordinal: 9 },
    };

    return (
      rules
        .filter((rule) => rule.isView === 1 && rule.menuCode)
        .map((rule) => {
          const config = menuConfigMap[rule.menuCode] || {
            name: rule.name || rule.menuCode,
            ordinal: 999,
          };
          return {
            key: rule.menuCode,
            id: rule.pathId,
            name: config.name,
            icon: this.getFallbackIcon(rule.menuCode),
            url: this.normalizeMenuUrl(rule.menuCode, rule.url),
            expanded: true,
            ordinal: config.ordinal,
          } as MenuItem & { ordinal: number };
        })
        .sort((a, b) => a.ordinal - b.ordinal)
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        .map(({ ordinal: _ordinal, ...item }) => item as MenuItem)
    );
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
