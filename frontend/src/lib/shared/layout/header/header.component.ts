import { Component, Injector, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@service';
import { MatDialog } from '@angular/material/dialog';
import { UserProfileDialogComponent } from '@components/user-profile-dialog/user-profile-dialog.component';
import { NotificationDetailDialogComponent } from './notification-detail-dialog/notification-detail-dialog.component';
import { Observable } from 'rxjs';
import { ICurrentUser } from '@model/auth.model';
import { UserInfoAction } from '@store/user-info';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';

interface HeaderNotificationItem {
  id: number;
  message: string;
  content: string;
  creator: string;
  time: string;
  read: boolean;
  alertId?: string;
  alertType?: string;
  severity?: string;
  objectType?: string;
  createdTimeAt?: string;
  expiryDate?: string;
  status?: number;
  sentFlag?: number;
  recipients?: string;
  sendMethod?: string;
}

@Component({
  selector: 'app-header-component',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  imports: [...MATERIAL_MODULE, TranslateModule, IconComponent, CommonModule],
})
export class HeaderComponent extends ComponentBaseAbstract implements OnInit {
  private readonly notificationReadStorageKey = 'he_thong_canh_bao_read_ids';
  private previousUnreadCount = 0;
  readonly brandLogoUrl = encodeURI(
    'config/Hệ thống quản lý lớp học Tiểu học.png'
  );

  /**
   * Nếu project bạn đang lưu user ở key khác thì thêm vào đây
   */
  private readonly AUTH_STORAGE_KEYS = [
    'currentUser',
    'user',
    'userInfo',
    'auth',
    'auth_user',
  ];

  notifications: HeaderNotificationItem[] = [];

  get unreadCount(): number {
    return this.notifications.filter((n) => !n.read).length;
  }

  // Thêm property để template có thể access user observable
  get user$(): Observable<ICurrentUser | null> {
    return this.authService.currentUser$ as Observable<ICurrentUser | null>;
  }

  constructor(
    protected override injector: Injector,
    private readonly authService: AuthService,
    private readonly matDialog: MatDialog
  ) {
    super(injector);
  }

  /**
   * Hỗ trợ cả trường hợp ApiService unwrap data hoặc giữ nguyên response gốc
   */

  /**
   * Ưu tiên lấy từ authService.currentUser
   * Nếu chưa có thì fallback qua localStorage theo cấu trúc bạn gửi: parsed.data.id
   */
  private getCurrentObjectId(): string {
    const authUserId = this.authService.currentUser?.id;
    if (authUserId !== null && authUserId !== undefined) {
      return String(authUserId);
    }

    for (const key of this.AUTH_STORAGE_KEYS) {
      const raw = localStorage.getItem(key);
      if (!raw) continue;

      try {
        const parsed = JSON.parse(raw);
        const id =
          parsed?.data?.id ??
          parsed?.id ??
          parsed?.user?.id ??
          parsed?.currentUser?.id;

        if (id !== null && id !== undefined && id !== '') {
          return String(id);
        }
      } catch {
        continue;
      }
    }

    return '';
  }

  private formatDateTime(value?: string): string {
    if (!value) return '';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(date);
  }

  private getReadNotificationIds(): number[] {
    const raw = localStorage.getItem(this.notificationReadStorageKey);
    if (!raw) return [];

    try {
      const ids = JSON.parse(raw);
      return Array.isArray(ids) ? ids.map(Number).filter(Boolean) : [];
    } catch {
      return [];
    }
  }

  private saveReadNotificationIds(ids: number[]) {
    const uniqueIds = Array.from(new Set(ids));
    localStorage.setItem(
      this.notificationReadStorageKey,
      JSON.stringify(uniqueIds)
    );
  }

  private addReadNotificationId(id: number) {
    const ids = this.getReadNotificationIds();
    if (ids.includes(id)) return;
    this.saveReadNotificationIds([...ids, id]);
  }

  markRead(item: HeaderNotificationItem) {
    item.read = true;
    this.addReadNotificationId(item.id);
  }

  markAllRead() {
    const allIds = this.notifications.map((n) => n.id);
    this.saveReadNotificationIds(allIds);
    this.notifications = this.notifications.map((n) => ({
      ...n,
      read: true,
    }));
  }

  openDetail(item: HeaderNotificationItem) {
    item.read = true;
    this.addReadNotificationId(item.id);

    this.matDialog.open(NotificationDetailDialogComponent, {
      width: '520px',
      autoFocus: false,
      data: {
        message: item.message,
        content: item.content,
        creator: item.creator,
        time: item.time,
        read: item.read,
        alertId: item.alertId,
        alertType: item.alertType,
        severity: item.severity,
        objectType: item.objectType,
        expiryDate: item.expiryDate,
        recipients: item.recipients,
        sendMethod: item.sendMethod,
      },
    });
  }

  showUserProfile() {
    const userInfo = this.authService.currentUser;
    this.matDialog.open(UserProfileDialogComponent, {
      width: '600px',
      maxHeight: '80vh',
      autoFocus: false,
      data: userInfo,
    });
  }

  logout() {
    this.authService.logout();
    // Clear store
    this.store.dispatch(
      UserInfoAction.Update({
        newState: {
          id: '',
          username: '',
          fullName: '',
          email: '',
          phone: '',
          status: 0,
          role: { id: 0, code: '', name: '', rules: [] },
          unit: { id: '', code: '', name: '' },
          permissions: { menus: [] },
        },
      })
    );
    // Navigate to login
    this.router.navigateByUrl(`/${NAVIGATOR_ENDPOINT.LOGIN}`);
  }
}
