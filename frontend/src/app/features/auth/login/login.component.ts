import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { MATERIAL_MODULE } from '@modules';
import {
  AuthService,
  DialogService,
  PermissionCheckService,
  ToastService,
} from '@service';
import { sha256 } from '@utils/utils';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, ...MATERIAL_MODULE],
})
export class LoginComponent {
  private readonly defaultRedirects = [
    {
      menuCode: 'ACCOUNT_MANAGEMENT',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH,
      ],
    },
    {
      menuCode: 'UNIT_MANAGEMENT',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.DON_VI.BASE_PATH,
      ],
    },
    {
      menuCode: 'ROLE_MANAGEMENT',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.VAI_TRO.BASE_PATH,
      ],
    },
    {
      menuCode: 'FUNCTION_MANAGEMENT',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.MENU.BASE_PATH,
      ],
    },
    {
      menuCode: 'SCHOOL_YEAR_CONFIG',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.NAM_HOC.BASE_PATH,
      ],
    },
    {
      menuCode: 'GRADE_CONFIG',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.KHOI.BASE_PATH,
      ],
    },
    {
      menuCode: 'CLASS_MANAGEMENT',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.LOP.BASE_PATH,
      ],
    },
    {
      menuCode: 'SUBJECT_MANAGEMENT',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.MON_HOC.BASE_PATH,
      ],
    },
    {
      menuCode: 'STUDENT_PROFILE',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.HOC_SINH.BASE_PATH,
      ],
    },
    {
      menuCode: 'STAFF_PROFILE',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.CAN_BO.BASE_PATH,
      ],
    },
  ] as const;

  readonly logoUrl = 'config/Logo_login.png';
  showPassword = false;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly dialogService: DialogService,
    private readonly permissionCheckService: PermissionCheckService,
    private readonly toastService: ToastService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    sha256(value.password ?? '').then((hashedPassword) => {
      this.authService
        .login({
          username: value.username ?? '',
          password: hashedPassword,
        })
        .subscribe({
          next: (user) => {
            void this.router.navigate(this.getFirstAccessibleRoute(user));
          },
          error: (error) => {
            const message =
              error?.error?.userMessage ??
              error?.error?.message ??
              error?.message ??
              'Đăng nhập thất bại';

            this.toastService.removeToastr();
            this.toastService.error(message, 'Thất bại');
          },
        });
    });
  }

  openForgotPassword(): void {
    this.dialogService.success({
      title: 'Quên mật khẩu',
      message: 'Vui lòng liên hệ quản trị viên để được cấp lại mật khẩu.',
      closeButtonText: 'Đóng',
      width: '420px',
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  private getFirstAccessibleRoute(user?: {
    role?: { rules?: Array<{ menuCode?: string; isView?: number }> };
    permissions?: {
      menus?: any[];
    };
  }): string[] {
    const userRules = user?.role?.rules ?? [];
    const visibleMenuCodes = new Set(
      this.flattenMenus(user?.permissions?.menus ?? [])
        .filter((menu: any) => Number(menu?.actions?.isView ?? 0) === 1)
        .map((menu: any) => (menu?.menuCode ?? '').trim().toUpperCase())
    );
    const firstAllowed = this.defaultRedirects.find(
      ({ menuCode }) =>
        this.permissionCheckService.canView(menuCode) ||
        visibleMenuCodes.has(menuCode) ||
        userRules.some(
          (rule) =>
            (rule.menuCode ?? '').trim().toUpperCase() === menuCode &&
            Number(rule.isView ?? 0) === 1
        )
    );

    return firstAllowed?.commands
      ? [...firstAllowed.commands]
      : [NAVIGATOR_ENDPOINT.ACCESS_DENIED];
  }

  private flattenMenus(menus: any[]): any[] {
    return (menus ?? []).flatMap((menu: any) => [
      menu,
      ...this.flattenMenus(menu?.children ?? []),
    ]);
  }
}
