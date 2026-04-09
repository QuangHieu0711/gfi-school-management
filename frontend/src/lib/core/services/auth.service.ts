/* eslint-disable @typescript-eslint/no-explicit-any */
import { HttpBackend, HttpClient, HttpHeaders, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  Observable,
  catchError,
  map,
  of,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import {
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
} from '@constant/constant';
import { environment } from '@env/environment';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import {
  AUTH_KEY,
  ICaptchaResponse,
  IChangePasswordRequest,
  IChangePasswordResponse,
  ICurrentUser,
  IRefreshTokenResponse,
  IRule,
  UserRole,
} from '@model/auth.model';
import { ID_TYPE, IResponse } from '@model/response.model';
import { StorageService } from '@service';

import { PermissionService } from './permission.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly forceChangePasswordKey = 'force_change_password';
  private readonly authBaseUrl = `${environment.host_api}/auth`;
  private readonly rawHttp: HttpClient;

  private readonly isLoggedInSubject = new BehaviorSubject<boolean>(false);
  private readonly currentUserSubject =
    new BehaviorSubject<ICurrentUser | null>(null);

  public isLoggedIn$ = this.isLoggedInSubject.asObservable();
  public currentUser$ = this.currentUserSubject.asObservable();

  get username(): string {
    return this.currentUserSubject.value?.username ?? '';
  }

  get currentUser(): ICurrentUser | null {
    return this.currentUserSubject.value;
  }

  get mustChangePassword(): boolean {
    return (
      this.storageService.get<boolean>(this.forceChangePasswordKey, 'all') ===
      true
    );
  }

  constructor(
    private readonly http: HttpClient,
    httpBackend: HttpBackend,
    private readonly storageService: StorageService,
    private readonly router: Router,
    private readonly permissionService: PermissionService
  ) {
    this.rawHttp = new HttpClient(httpBackend);
    this.restoreStoredSession();
  }

  getAccessToken(): string {
    return this.storageService.get<string>(ACCESS_TOKEN_KEY, 'all') ?? '';
  }

  getRefreshToken(): string {
    return this.storageService.get<string>(REFRESH_TOKEN_KEY, 'all') ?? '';
  }

  addTokenHeader<T>(request: HttpRequest<T>, token?: string): HttpRequest<T> {
    if (!token) return request;

    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  isAuthenticated(): boolean {
    return !!this.currentUserSubject.value || this.storageService.has('userInfo', 'all');
  }

  isTokenExpired(_token?: string): boolean {
    return false;
  }

  setAccessToken(token: string, rememberMe = false): void {
    this.storageService.set(
      ACCESS_TOKEN_KEY,
      token,
      rememberMe ? 'local' : 'session'
    );
  }

  setRefreshToken(token: string, rememberMe = false): void {
    this.storageService.set(
      REFRESH_TOKEN_KEY,
      token,
      rememberMe ? 'local' : 'session'
    );
  }

  clearTokens(): void {
    this.storageService.remove(ACCESS_TOKEN_KEY, 'all');
    this.storageService.remove(REFRESH_TOKEN_KEY, 'all');
  }

  getCaptcha(): Observable<ICaptchaResponse> {
    return of({
      imageBase64: '',
      key: 'local-captcha-disabled',
    });
  }

  login(
    payload: {
      [AUTH_KEY.USERNAME]: string;
      [AUTH_KEY.PASSWORD]: string;
      deviceType: string;
      captchaCode?: string;
    },
    rememberMe = false
  ): Observable<ICurrentUser> {
    return this.rawHttp
      .post<BackendLoginEnvelope>(`${this.authBaseUrl}/login`, {
        username: payload.username,
        password: payload.password,
      })
      .pipe(
        switchMap((response) =>
          this.fetchRulesByRoleId(
            response?.data?.roleId,
            response?.data?.accessToken ?? ''
          ).pipe(
            map((rules) =>
              this.mapLoginResponseToUser(response, payload.username, rules)
            )
          )
        ),
        tap((user) => this.setLocalSession(user, rememberMe))
      );
  }

  logout(): void {
    this.handleLogout();
  }

  handleLogout(): void {
    this.clearTokens();
    this.clearMustChangePassword();
    this.storageService.remove('userInfo', 'all');

    this.permissionService.setRules([]);
    this.currentUserSubject.next(null);
    this.isLoggedInSubject.next(false);

    void this.router.navigate([NAVIGATOR_ENDPOINT.LOGIN]);
  }

  refreshToken(): Observable<IRefreshTokenResponse> {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    if (!accessToken || !refreshToken) {
      return throwError(() => new Error('Refresh token not available'));
    }

    return of({
      accessToken,
      refreshToken,
      isFirstLogin: false,
    } as IRefreshTokenResponse);
  }

  changePassword(_payload: IChangePasswordRequest): Observable<IChangePasswordResponse> {
    this.clearMustChangePassword();
    return of({
      success: true,
      message: 'Password changed locally',
    });
  }

  getCurrentUser(): Observable<ICurrentUser> {
    const user =
      this.currentUserSubject.value ??
      this.storageService.get<ICurrentUser>('userInfo', 'all');

    if (!user) {
      return of(null as unknown as ICurrentUser);
    }

    this.setLocalSession(user, !!user.rememberMe);
    return of(user);
  }

  clearMustChangePassword(): void {
    this.storageService.remove(this.forceChangePasswordKey, 'all');
  }

  markMustChangePassword(): void {
    this.setMustChangePassword(true);
  }

  getUserRules(): Observable<IRule[]> {
    return of(this.currentUserSubject.value?.role.rules ?? []);
  }

  resetPassword(_accountId: ID_TYPE): Observable<any> {
    return of({
      success: true,
      message: 'Reset password locally',
    });
  }

  private setMustChangePassword(value: boolean): void {
    if (value) {
      this.storageService.set(this.forceChangePasswordKey, true, 'all');
      return;
    }

    this.clearMustChangePassword();
  }

  private restoreStoredSession(): void {
    const storedUser = this.storageService.get<ICurrentUser>('userInfo', 'all');
    if (!storedUser) return;

    this.currentUserSubject.next(storedUser);
    this.isLoggedInSubject.next(true);
    this.permissionService.setRules(storedUser.role.rules ?? []);
  }

  private setLocalSession(user: ICurrentUser, rememberMe: boolean): void {
    const userWithRemember = { ...user, rememberMe };

    this.setAccessToken((user as ICurrentUserWithTokens).accessToken ?? '', rememberMe);
    this.setRefreshToken((user as ICurrentUserWithTokens).refreshToken ?? '', rememberMe);
    this.storageService.set(
      'userInfo',
      userWithRemember,
      rememberMe ? 'local' : 'session'
    );

    this.currentUserSubject.next(userWithRemember);
    this.isLoggedInSubject.next(true);
    this.permissionService.setRules(userWithRemember.role.rules ?? []);
  }

  private createFakeUser(username = 'admin'): ICurrentUser {
    return {
      id: 1,
      username,
      name: 'Local Admin',
      email: 'admin@local.dev',
      role: {
        id: 1,
        name: UserRole.ADMIN,
        rules: this.getDefaultRules(),
      },
      donVi: {
        maDonVi: 'DV001',
        tenDonVi: 'Don vi local',
        tenVietTat: 'LOCAL',
        role: [],
      },
      rememberMe: true,
    };
  }

  private mapLoginResponseToUser(
    response: BackendLoginEnvelope,
    username: string,
    rules: IRule[]
  ): ICurrentUserWithTokens {
    const data = response?.data;
    const normalizedRole = this.normalizeRoleName(data?.role);

    return {
      id: data?.userId ?? 0,
      username,
      name: data?.fullName ?? username,
      email: '',
      role: {
        id: data?.roleId ?? 0,
        name: normalizedRole,
        rules,
      },
      donVi: {
        maDonVi: '',
        tenDonVi: '',
        tenVietTat: '',
        role: [],
      },
      accessToken: data?.accessToken ?? '',
      refreshToken: data?.refreshToken ?? '',
    };
  }

  private fetchRulesByRoleId(
    roleId: ID_TYPE | undefined,
    accessToken: string
  ): Observable<IRule[]> {
    if (roleId == null || accessToken.trim() === '') {
      return of([]);
    }

    return this.rawHttp
      .get<IResponse<BackendPermissionItem[]>>(
        `${environment.host_api}/permissions/${roleId}`,
        {
          headers: new HttpHeaders({
            Authorization: `Bearer ${accessToken}`,
          }),
        }
      )
      .pipe(
        map((response) => this.mapPermissionsToRules(response?.data ?? [])),
        catchError(() => of([]))
      );
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
        isView: Number(item.isView ?? 0),
        isAdd: Number(item.isAdd ?? 0),
        isEdit: Number(item.isEdit ?? 0),
        isDelete: Number(item.isDelete ?? 0),
        isDownload: Number(item.isDownload ?? 0),
        isApprove: 0,
        name: item.menuName ?? '',
        url: item.menuUrl ?? '',
        pid:
          item.parentId == null || item.parentId === ''
            ? undefined
            : Number(item.parentId),
        pathId: String(item.menuId ?? ''),
        ordinal: Number(item.ordinal ?? 0),
        icon: item.icon ?? '',
      }));
  }

  private normalizeRoleName(role?: string): UserRole {
    return role?.toUpperCase() === 'ROLE_ADMIN' ? UserRole.ADMIN : UserRole.ADMIN;
  }

  private getDefaultRules(): IRule[] {
    return [
      {
        ruleId: 1,
        roleId: 1,
        moduleId: 1,
        isView: 1,
        isAdd: 1,
        isEdit: 1,
        isDelete: 1,
        isDownload: 1,
        isApprove: 1,
        name: 'Nguoi dung',
        url: 'NguoiDung',
        pathId: '1',
        ordinal: 1,
        icon: 'group',
        pid: 0,
      },
    ];
  }
}

interface BackendLoginEnvelope {
  code?: number;
  data?: {
    accessToken?: string;
    refreshToken?: string;
    tokenType?: string;
    expiresIn?: number;
    role?: string;
    roleId?: number;
    roleName?: string;
    fullName?: string;
    userId?: number;
  };
}

interface ICurrentUserWithTokens extends ICurrentUser {
  accessToken?: string;
  refreshToken?: string;
}

interface BackendPermissionItem {
  id?: ID_TYPE | null;
  roleId?: ID_TYPE;
  menuId?: ID_TYPE;
  menuName?: string | null;
  menuUrl?: string | null;
  parentId?: ID_TYPE | null;
  icon?: string | null;
  ordinal?: number | null;
  isView?: number;
  isAdd?: number;
  isEdit?: number;
  isDelete?: number;
  isDownload?: number;
}
