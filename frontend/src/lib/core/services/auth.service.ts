/* eslint-disable @typescript-eslint/no-explicit-any */
import { HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';

import {
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
} from '@constant/constant';
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
import { ID_TYPE } from '@model/response.model';
import { StorageService } from '@service';

import { PermissionService } from './permission.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly forceChangePasswordKey = 'force_change_password';
  private readonly fakeAccessToken = 'local-access-token';
  private readonly fakeRefreshToken = 'local-refresh-token';

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
    private readonly storageService: StorageService,
    private readonly router: Router,
    private readonly permissionService: PermissionService
  ) {
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
    const user = this.createFakeUser(payload.username || 'admin');
    this.setLocalSession(user, rememberMe);
    return of(user);
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
    this.setAccessToken(this.fakeAccessToken, true);
    this.setRefreshToken(this.fakeRefreshToken, true);

    return of({
      accessToken: this.fakeAccessToken,
      refreshToken: this.fakeRefreshToken,
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
      return throwError(() => new Error('User not found'));
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
    return of(this.getDefaultRules());
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

    this.setAccessToken(this.fakeAccessToken, rememberMe);
    this.setRefreshToken(this.fakeRefreshToken, rememberMe);
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
