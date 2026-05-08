import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ICurrentUser, IMenuPermission } from '@model/auth.model';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { MATERIAL_MODULE } from '@modules';
import {
  AuthService,
  PermissionCheckService,
  ToastService,
} from '@service';
import { sha256 } from '@utils/utils';

@Component({
  selector: 'app-change-password',
  standalone: true,
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, ...MATERIAL_MODULE],
})
export class ChangePasswordComponent {
  private readonly defaultRedirects = [
    {
      menuCode: 'HOME',
      commands: [
        NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
        NAVIGATOR_ENDPOINT.ADMIN.DASHBOARD.BASE_PATH,
      ],
    },
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
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly permissionCheckService: PermissionCheckService,
    private readonly toastService: ToastService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_])[A-Za-z\d\W_]{8,}$/
          ),
        ],
      ],
      confirmPassword: ['', Validators.required],
    });

    this.form.get('confirmPassword')?.addValidators((control) => {
      const newPassword = this.form?.get('newPassword')?.value;
      return newPassword === control.value ? null : { mismatch: true };
    });

    this.form.get('newPassword')?.valueChanges.subscribe(() => {
      if (this.form.get('confirmPassword')?.value) {
        this.form.get('confirmPassword')?.updateValueAndValidity({ emitEvent: false });
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    Promise.all([
      sha256(value.currentPassword ?? ''),
      sha256(value.newPassword ?? '')
    ]).then(([currentHashed, newHashed]) => {
      this.authService
        .changePassword({
          currentPassword: currentHashed,
          newPassword: newHashed,
        })
        .subscribe({
          next: () => {
            this.toastService.success('Đổi mật khẩu thành công', 'Thành công');
            const user = this.authService.currentUser;
            if (user) {
              user.mustChangePassword = false;
              this.authService.setLocalSession(user, false);
              void this.router.navigate(this.getFirstAccessibleRoute(user));
            } else {
              void this.router.navigate(['/login']);
            }
          },
          error: (error) => {
            const message =
              error?.error?.userMessage ??
              error?.error?.message ??
              error?.message ??
              'Đổi mật khẩu thất bại';

            this.toastService.removeToastr();
            this.toastService.error(message, 'Thất bại');
          },
        });
    });
  }

  toggleCurrentPasswordVisibility(): void {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPasswordVisibility(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  private getFirstAccessibleRoute(
    user?: Pick<ICurrentUser, 'role' | 'permissions'>
  ): string[] {
    const userRules = user?.role?.rules ?? [];
    const visibleMenuCodes = new Set(
      this.flattenMenus(user?.permissions?.menus ?? [])
        .filter((menu) => Number(menu.actions?.isView ?? 0) === 1)
        .map((menu) => (menu.menuCode ?? '').trim().toUpperCase())
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
      ? this.normalizeRouteCommands(firstAllowed.commands)
      : [NAVIGATOR_ENDPOINT.ACCESS_DENIED];
  }

  private normalizeRouteCommands(commands: readonly string[]): string[] {
    return commands.flatMap((command) =>
      String(command)
        .split('/')
        .filter((segment) => segment.trim() !== '')
    );
  }

  private flattenMenus(menus: IMenuPermission[]): IMenuPermission[] {
    return (menus ?? []).flatMap((menu) => [
      menu,
      ...this.flattenMenus(menu.children ?? []),
    ]);
  }
}
