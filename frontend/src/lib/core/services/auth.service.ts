/* eslint-disable @typescript-eslint/no-explicit-any */
import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpHeaders,
  HttpRequest,
} from '@angular/common/http';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import {
  BehaviorSubject,
  catchError,
  map,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import {
  ACCESS_TOKEN_KEY,
  AUTH_CHANNEL,
  REFRESH_TOKEN_KEY,
} from '@constant/constant';

import {
  AUTH_API_ENDPOINT,
  AUTH_KEY,
  AuthMessage,
  ICurrentUser,
  ILoginResponse,
  IRefreshTokenResponse,
  IChangePasswordRequest,
  IChangePasswordResponse,
  IRule,
} from '@model/auth.model';

import { ID_TYPE, IResponse } from '@model/response.model';
import { ApiService, StorageService } from '@service';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

import { UserData } from '@features/admin/admin.interface';
import { environment } from '@env/environment';
import { PermissionService } from './permission.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly forceChangePasswordKey = 'force_change_password';
  private readonly authChannel = new BroadcastChannel(AUTH_CHANNEL);

  private readonly isLoggedInSubject = new BehaviorSubject<boolean>(false);
  private readonly currentUserSubject =
    new BehaviorSubject<ICurrentUser | null>(null);

  private readonly uuid = crypto.randomUUID();

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
    private readonly apiService: ApiService,
    private readonly storageService: StorageService,
    private readonly router: Router,
    private readonly permissionService: PermissionService
  ) {
    this.isLoggedInSubject.next(this.isAuthenticated());

    const storedUser = this.storageService.get<ICurrentUser>('userInfo', 'all');

    if (storedUser && this.isAuthenticated()) {
      this.currentUserSubject.next(storedUser);

      const rules = (storedUser?.role?.rules ?? []) as IRule[];
      this.permissionService.setRules(rules);
    }

    this.listenToAuthChannel();
    this.requestSessionFromOtherTabs();
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
    const token = this.getAccessToken();
    if (!token) return false;

    const decoded = jwtDecode(token);
    if (!decoded.exp) return false;

    return Date.now() < decoded.exp * 1000;
  }

  isTokenExpired(token?: string): boolean {
    if (!token) return true;

    const decoded = jwtDecode(token);
    if (!decoded.exp) return true;

    return Date.now() > decoded.exp * 1000;
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

  getCaptcha() {
    return this.apiService.post<{ imageBase64: string; key: string }>(
      AUTH_API_ENDPOINT.CAPTCHA,
      {},
      undefined,
      { routerHost: environment.auth }
    );
  }

  login(
    payload: {
      [AUTH_KEY.USERNAME]: string;
      [AUTH_KEY.PASSWORD]: string;
      deviceType: string;
      captchaCode?: string;
    },
    rememberMe = false
  ) {
    return this.apiService
      .post<ILoginResponse>(AUTH_API_ENDPOINT.AUTH_TOKEN, payload, undefined, {
        routerHost: environment.auth,
        customError: { silent: true },
      })
      .pipe(
        tap((tokens: IResponse<ILoginResponse>) => {
          this.setAccessToken(tokens.data.accessToken, rememberMe);
          this.setRefreshToken(tokens.data.refreshToken, rememberMe);

          const mustChangePasswordByLogin = this.isFirstLoginRequired(
            tokens.data?.isFirstLogin
          );
          this.setMustChangePassword(mustChangePasswordByLogin);
        }),
        switchMap(() => this.getCurrentUser()),
        tap((user) => {
          const userWithRememberMe = { ...user, rememberMe };

          this.storageService.set(
            'userInfo',
            userWithRememberMe,
            rememberMe ? 'local' : 'session'
          );

          this.currentUserSubject.next(userWithRememberMe);

          const rules = user?.role?.rules ?? ([] as IRule[]);
          this.permissionService.setRules(rules);

          this.authChannel.postMessage({
            type: 'LOGIN',
            accessToken: this.getAccessToken(),
            refreshToken: this.getRefreshToken(),
            rememberMe,
            uuid: this.uuid,
          });
        }),
        catchError((error: HttpErrorResponse) => throwError(() => error))
      );
  }

  logout() {
    const refreshToken = this.getRefreshToken();

    this.apiService
      .delete(
        AUTH_API_ENDPOINT.AUTH_TOKEN,
        { token: refreshToken },
        { routerHost: environment.auth }
      )
      .subscribe(() => {
        this.handleLogout();
      });
  }

  handleLogout() {
    this.clearTokens();
    this.clearMustChangePassword();

    this.storageService.remove('userInfo', 'all');

    this.currentUserSubject.next(null);
    this.isLoggedInSubject.next(false);

    this.router.navigate([NAVIGATOR_ENDPOINT.LOGIN]);

    this.authChannel.postMessage({ type: 'LOGOUT', uuid: this.uuid });
  }

  refreshToken() {
    const refreshToken = this.getRefreshToken();

    if (!refreshToken) {
      this.logout();
      throw new Error('Refresh token not found');
    }

    return this.apiService
      .post<IRefreshTokenResponse>(
        AUTH_API_ENDPOINT.REFRESH_TOKEN,
        { routerHost: environment.auth },
        { token: refreshToken }
      )
      .pipe(
        tap((tokens: IResponse<IRefreshTokenResponse>) => {
          this.setAccessToken(tokens.data.accessToken);
          this.setRefreshToken(tokens.data.refreshToken);
        }),
        catchError((error: HttpErrorResponse) => throwError(() => error))
      );
  }

  changePassword(payload: IChangePasswordRequest) {
    return this.apiService
      .post<IChangePasswordResponse>(
        AUTH_API_ENDPOINT.CHANGE_PASSWORD,
        payload,
        undefined,
        { routerHost: environment.auth }
      )
      .pipe(catchError((error: HttpErrorResponse) => throwError(() => error)));
  }

  getCurrentUser() {
    const accessToken = this.getAccessToken();

    if (!accessToken) {
      return throwError(() => new Error('Access token not found'));
    }

    return this.apiService
      .get<ICurrentUser>(AUTH_API_ENDPOINT.AUTH_USER, {
        routerHost: environment.auth,
      })
      .pipe(
        map((response: IResponse<ICurrentUser>) => response.data),

        tap((user: ICurrentUser) => {
          this.currentUserSubject.next(user);
          this.isLoggedInSubject.next(true);

          const rules = user?.role?.rules ?? ([] as IRule[]);
          this.permissionService.setRules(rules);
        }),

        catchError((error) => {
          this.currentUserSubject.next(null);
          return throwError(() => error);
        })
      );
  }

  clearMustChangePassword(): void {
    this.storageService.remove(this.forceChangePasswordKey, 'all');
  }

  markMustChangePassword(): void {
    this.setMustChangePassword(true);
  }

  private setMustChangePassword(value: boolean): void {
    if (value) {
      this.storageService.set(this.forceChangePasswordKey, true, 'all');
      return;
    }

    this.clearMustChangePassword();
  }

  private isFirstLoginRequired(value: unknown): boolean {
    if (value === 1 || value === '1' || value === true) {
      return true;
    }

    if (typeof value === 'number') {
      return false;
    }

    if (typeof value === 'string') {
      const normalized = value.trim().toLowerCase();
      return (
        normalized === 'true' || normalized === 'yes' || normalized === 'y'
      );
    }

    return false;
  }

  private listenToAuthChannel() {
    this.authChannel.onmessage = (event: MessageEvent<AuthMessage>) => {
      const msgData = event.data;
      const { type, uuid } = msgData;

      if (uuid === this.uuid) return;

      switch (type) {
        case 'LOGIN':
          this.setAccessToken(msgData.accessToken, msgData.rememberMe);
          this.setRefreshToken(msgData.refreshToken, msgData.rememberMe);
          this.isLoggedInSubject.next(true);
          break;

        case 'LOGOUT':
          sessionStorage.clear();
          this.isLoggedInSubject.next(false);
          break;

        case 'REQUEST_SESSION':
          this.authChannel.postMessage({
            type: 'SYNC_SESSION',
            accessToken: this.getAccessToken(),
            refreshToken: this.getRefreshToken(),
            sender: this.uuid,
          });
          break;

        case 'SYNC_SESSION':
          this.setAccessToken(msgData.accessToken, false);
          this.setRefreshToken(msgData.refreshToken, false);
          this.isLoggedInSubject.next(true);
          break;
      }
    };
  }

  private requestSessionFromOtherTabs() {
    if (!this.isAuthenticated()) {
      this.authChannel.postMessage({
        type: 'REQUEST_SESSION',
        uuid: this.uuid,
      });
    }
  }

  getUserRules() {
    const customHeaders = new HttpHeaders({
      'Content-Type': 'application/json',
      'X-Username': 'testuser',
      'X-User-Id': '123',
    });

    return this.apiService
      .get<UserData>('auth/user', {
        routerHost: environment.auth,
        headers: customHeaders,
        customError: { silent: true },
      })
      .pipe(
        map((response: IResponse<UserData>) => {
          return response?.data?.role?.rules ?? [];
        }),
        catchError((error) => throwError(() => error))
      );
  }

  resetPassword(accountId: ID_TYPE) {
    const payload = { accountId };

    return this.apiService
      .post<any>('public/auth/reset-password', payload, undefined, {
        routerHost: environment.auth,
      })
      .pipe(catchError((error: HttpErrorResponse) => throwError(() => error)));
  }
}
