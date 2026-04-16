/* eslint-disable @typescript-eslint/no-explicit-any */
import { BehaviorSubject, Observable, of, map, catchError } from 'rxjs';
import { HttpRequest, HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@constant/constant';
import { PermissionCheckService, StorageService } from '@service';
import {
  AUTH_API_ENDPOINT,
  ICurrentUser,
  IMenuPermission,
  IRule,
} from '@model/auth.model';

import { UserPermission } from './permission-check.service';

interface ICurrentUserWithTokens extends ICurrentUser {
  accessToken: string;
  refreshToken: string;
}

interface BackendPermissionItem {
  id?: number;
  menuId?: number;
  roleId?: number;
  menuCode?: string;
  menuName?: string;
  menuUrl?: string;
  parentId?: number;
  icon?: string;
  ordinal?: number;
  isView?: number;
  isAdd?: number;
  isEdit?: number;
  isDelete?: number;
  isDownload?: number;
  isConfig?: number;
}

interface BackendLoginStaff {
  id?: number | string;
  staffCode?: string;
  fullName?: string;
  email?: string | null;
  phone?: string;
  unit?: {
    id?: number | string;
    code?: string;
    name?: string;
  } | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserSubject =
    new BehaviorSubject<ICurrentUserWithTokens | null>(null);
  public readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private storageService: StorageService,
    private http: HttpClient,
    private permissionCheckService: PermissionCheckService
  ) {}

  // Restore session from storage into memory
  restoreStoredSession(): void {
    const userInfo = this.storageService.get(
      'userInfo',
      'all'
    ) as ICurrentUserWithTokens | null;
    if (userInfo) {
      this.currentUserSubject.next(userInfo);
      this.permissionCheckService.setPermissions(
        this.mapRulesToPermissions(userInfo.role?.rules ?? [])
      );
      // Menus Ä‘Ã£ Ä‘Æ°á»£c lÆ°u trong userInfo.permissions.menus, component sáº½ láº¥y tá»« store
    }
  }

  mapLoginResponseToUser(
    response: any,
    username: string,
    rules: IRule[]
  ): ICurrentUserWithTokens {
    const token = response?.data?.token ?? {};
    const user = response?.data?.user ?? {};
    const staff = this.normalizeStaff(user.staff);
    const menus = this.normalizeMenus(response?.data?.permissions?.menus ?? []);
    return {
      id: user.id,
      username,
      fullName: staff?.fullName ?? user.username ?? username,
      email: staff?.email ?? null,
      phone: staff?.phone ?? '',
      status: user.status,
      role: {
        id: user.role?.id,
        code: user.role?.code,
        name: user.role?.name,
        rules,
      },
      unit: staff?.unit ?? null,
      staff,
      lastLoginAt: user.lastLoginAt ?? null,
      permissions: { menus },
      accessToken: String(token.accessToken ?? ''),
      refreshToken: String(token.refreshToken ?? ''),
    };
  }

  setLocalSession(user: ICurrentUserWithTokens, rememberMe: boolean): void {
    const remember = !!rememberMe;
    this.storageService.set(
      ACCESS_TOKEN_KEY,
      user.accessToken,
      remember ? 'local' : 'session'
    );
    this.storageService.set(
      REFRESH_TOKEN_KEY,
      user.refreshToken,
      remember ? 'local' : 'session'
    );
    this.storageService.set(
      'userInfo',
      { ...user, rememberMe: remember },
      remember ? 'local' : 'session'
    );
    this.currentUserSubject.next(user);
    this.permissionCheckService.setPermissions(
      this.mapRulesToPermissions(user.role?.rules ?? [])
    );
  }

  // Public API expected by the rest of the app
  get currentUser(): ICurrentUserWithTokens | null {
    return this.currentUserSubject.value;
  }

  getAccessToken(): string {
    return (
      this.currentUser?.accessToken ??
      (this.storageService.get(ACCESS_TOKEN_KEY) as string) ??
      ''
    );
  }

  addTokenHeader<T>(request: HttpRequest<T>, token?: string): HttpRequest<T> {
    if (!token) return request;
    try {
      return request.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      });
    } catch {
      return request;
    }
  }

  isAuthenticated(): boolean {
    return !!this.currentUser || !!this.storageService.get(ACCESS_TOKEN_KEY);
  }

  getCurrentUser(): Observable<ICurrentUserWithTokens | null> {
    const user = this.currentUserSubject.value;
    if (!user) return of(null);
    return of(user);
  }

  mustChangePassword = false;

  logout(): void {
    this.handleLogout();
  }

  handleLogout(): void {
    this.storageService.remove(ACCESS_TOKEN_KEY, 'all');
    this.storageService.remove(REFRESH_TOKEN_KEY, 'all');
    this.storageService.remove('userInfo', 'all');
    this.currentUserSubject.next(null);
    this.permissionCheckService.clearPermissions();
  }

  refreshToken(): Observable<{
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresAt: number;
  }> {
    const accessToken = this.getAccessToken();
    const refreshToken =
      this.currentUser?.refreshToken ??
      (this.storageService.get(REFRESH_TOKEN_KEY) as string) ??
      '';
    return of({ accessToken, refreshToken, tokenType: 'Bearer', expiresAt: 0 });
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  resetPassword(_accountId: number): Observable<any> {
    return of({ success: true, message: 'Password reset locally' });
  }

  login(
    payload: {
      username: string;
      password: string;
      captchaCode?: string;
    },
    rememberMe = false
  ): Observable<ICurrentUserWithTokens> {
    return this.http
      .post<any>(`/api/${AUTH_API_ENDPOINT.AUTH_TOKEN}`, payload)
      .pipe(
        map((response: any) => {
          // Extract rules from permissions
          const menus = response?.data?.permissions?.menus ?? [];
          const rules = this.mapMenusToRules(menus);

          // Map response to user with tokens
          const user = this.mapLoginResponseToUser(
            response,
            payload.username,
            rules
          );

          // Store user and tokens in storage and subject
          this.setLocalSession(user, rememberMe);

          return user;
        }),
        catchError((error: any) => {
          console.error('Login error:', error);
          throw error;
        })
      );
  }

  private mapMenusToRules(
    menus: {
      menuCode?: string;
      menuName?: string;
      path?: string;
      actions?: {
        isView?: number;
        isAdd?: number;
        isEdit?: number;
        isDelete?: number;
        isDownload?: number;
        isConfig?: number;
      };
      icon?: string;
      level?: number;
      dataScopes?: { scopeType?: string; scopeValues?: number[] }[];
    }[]
  ): IRule[] {
    const flatMenus = this.flattenMenus(menus);

    return flatMenus.map((menu, index) => ({
      ruleId: index + 1,
      roleId: 0,
      moduleId: index + 1,
      menuCode: menu.menuCode ?? '',
      isView: menu.actions?.isView ?? 0,
      isAdd: menu.actions?.isAdd ?? 0,
      isEdit: menu.actions?.isEdit ?? 0,
      isDelete: menu.actions?.isDelete ?? 0,
      isDownload: menu.actions?.isDownload ?? 0,
      isConfig: menu.actions?.isConfig ?? 0,
      isApprove: 0,
      name: menu.menuName ?? this.getMenuDisplayName(menu.menuCode ?? ''),
      url: menu.path ?? '',
      pathId: String(index + 1),
      ordinal: index,
      icon: menu.icon ?? this.getMenuIcon(menu.menuCode ?? ''),
      pid: undefined,
      dataScopes: (menu.dataScopes ?? []).map((ds: any) => ({
        scopeType: ds.scopeType ?? 'ALL',
        scopeValues: ds.scopeValues ?? [],
      })),
    }));
  }

  private flattenMenus(menus: any[]): any[] {
    return (menus ?? []).flatMap((menu: any) => [
      menu,
      ...this.flattenMenus(menu.children ?? []),
    ]);
  }

  private normalizeMenus(menus: any[]): IMenuPermission[] {
    return (menus ?? []).map((menu) => ({
      menuCode: String(menu?.menuCode ?? ''),
      menuName: menu?.menuName ?? this.getMenuDisplayName(menu?.menuCode ?? ''),
      path: menu?.path ?? null,
      icon: menu?.icon ?? this.getMenuIcon(menu?.menuCode ?? ''),
      level: Number(menu?.level ?? 0),
      parentMenuId: menu?.parentMenuId ?? null,
      actions: {
        isView: Number(menu?.actions?.isView ?? 0),
        isAdd: Number(menu?.actions?.isAdd ?? 0),
        isEdit: Number(menu?.actions?.isEdit ?? 0),
        isDelete: Number(menu?.actions?.isDelete ?? 0),
        isDownload: Number(menu?.actions?.isDownload ?? 0),
        isConfig: Number(menu?.actions?.isConfig ?? 0),
      },
      dataScopes: (menu?.dataScopes ?? []).map((ds: any) => ({
        scopeType: ds?.scopeType ?? 'ALL',
        scopeValues: ds?.scopeValues ?? [],
      })),
      children: this.normalizeMenus(menu?.children ?? []),
    }));
  }

  private normalizeStaff(staff: BackendLoginStaff | null | undefined) {
    if (!staff) return null;

    return {
      id: staff.id ?? '',
      staffCode: staff.staffCode ?? '',
      fullName: staff.fullName ?? '',
      email: staff.email ?? null,
      phone: staff.phone ?? '',
      unit: staff.unit
        ? {
            id: staff.unit.id ?? '',
            code: staff.unit.code ?? '',
            name: staff.unit.name ?? '',
          }
        : null,
    };
  }

  private getMenuDisplayName(menuCode: string): string {
    const map: Record<string, string> = {
      USER_ADMIN: 'Quáº£n trá»‹ ngÆ°á»i dÃ¹ng',
      ACCOUNT_MANAGEMENT: 'Quáº£n lÃ½ tÃ i khoáº£n',
      UNIT_MANAGEMENT: 'Quáº£n lÃ½ Ä‘Æ¡n vá»‹',
      SYSTEM_CONFIG: 'Cáº¥u hÃ¬nh há»‡ thá»‘ng',
      ROLE_MANAGEMENT: 'Quáº£n lÃ½ vai trÃ²',
      FUNCTION_MANAGEMENT: 'Quáº£n lÃ½ chá»©c nÄƒng',
      SCHOOL_YEAR_CONFIG: 'Cáº¥u hÃ¬nh nÄƒm há»c',
      GRADE_CONFIG: 'Cáº¥u hÃ¬nh khá»‘i',
      ACADEMIC_MANAGEMENT: 'Quáº£n lÃ½ há»c táº­p',
      CLASS_MANAGEMENT: 'Quáº£n lÃ½ lá»›p',
      SUBJECT_MANAGEMENT: 'Quáº£n lÃ½ mÃ´n há»c',
      STUDENT: 'Há»c sinh',
      STUDENT_PROFILE: 'Há»“ sÆ¡ há»c sinh',
      STAFF_PROFILE: 'Ho so can bo',
    };

    return map[menuCode] ?? menuCode;
  }

  private getMenuIcon(menuCode: string): string {
    const map: Record<string, string> = {
      USER_ADMIN: 'groups',
      ACCOUNT_MANAGEMENT: 'account_circle',
      UNIT_MANAGEMENT: 'apartment',
      SYSTEM_CONFIG: 'settings',
      ROLE_MANAGEMENT: 'manage_accounts',
      FUNCTION_MANAGEMENT: 'account_tree',
      SCHOOL_YEAR_CONFIG: 'calendar_month',
      GRADE_CONFIG: 'menu_book',
      ACADEMIC_MANAGEMENT: 'school',
      CLASS_MANAGEMENT: 'meeting_room',
      SUBJECT_MANAGEMENT: 'book',
      STUDENT: 'school',
      STUDENT_PROFILE: 'badge',
      STAFF_PROFILE: 'supervisor_account',
    };

    return map[menuCode] ?? 'menu';
  }

  private mapPermissionsToRules(items: BackendPermissionItem[]): IRule[] {
    return [...items]
      .sort((a, b) => {
        const ordinalCompare = Number(a.ordinal ?? 0) - Number(b.ordinal ?? 0);
        if (ordinalCompare !== 0) return ordinalCompare;
        return Number(a.menuId ?? 0) - Number(b.menuId ?? 0);
      })
      .map((item) => ({
        ruleId: Number(item.id ?? item.menuId ?? 0),
        roleId: Number(item.roleId ?? 0),
        moduleId: item.menuId ?? 0,
        menuCode: item.menuCode ?? '',
        isView: Number(item.isView ?? 0),
        isAdd: Number(item.isAdd ?? 0),
        isEdit: Number(item.isEdit ?? 0),
        isDelete: Number(item.isDelete ?? 0),
        isDownload: Number(item.isDownload ?? 0),
        isConfig: Number(item.isConfig ?? 0),
        isApprove: 0,
        name: item.menuName ?? '',
        url: item.menuUrl ?? '',
        pid: item.parentId ?? undefined,
        pathId: String(item.menuId ?? ''),
        ordinal: Number(item.ordinal ?? 0),
        icon: item.icon ?? '',
        dataScopes: [],
      }));
  }

  private mapRulesToPermissions(rules: IRule[]): UserPermission[] {
    return rules.map((rule) => ({
      id: rule.ruleId,
      roleId: rule.roleId,
      menuId: rule.moduleId,
      menuCode: rule.menuCode ?? '',
      menuName: rule.name,
      menuUrl: rule.url,
      parentId: rule.pid ?? null,
      icon: rule.icon,
      ordinal: rule.ordinal,
      isView: Number(rule.isView ?? 0),
      isAdd: Number(rule.isAdd ?? 0),
      isEdit: Number(rule.isEdit ?? 0),
      isDelete: Number(rule.isDelete ?? 0),
      isDownload: Number(rule.isDownload ?? 0),
      isConfig: Number(rule.isConfig ?? 0),
      dataScopes: (rule.dataScopes ?? []).length ? rule.dataScopes : [],
    }));
  }
}

