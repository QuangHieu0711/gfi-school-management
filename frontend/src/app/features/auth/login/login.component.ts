import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
import { MATERIAL_MODULE } from '@modules';
import { AuthService, DialogService, ToastService } from '@service';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, ...MATERIAL_MODULE],
})
export class LoginComponent {
  readonly logoUrl = 'config/Logo_login.png';
  showPassword = false;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly dialogService: DialogService,
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
    this.authService
      .login({
        username: value.username ?? '',
        password: value.password ?? '',
        deviceType: 'web',
      })
      .subscribe({
        next: () => {
          void this.router.navigate([
            NAVIGATOR_ENDPOINT.ADMIN.BASE_PATH,
            NAVIGATOR_ENDPOINT.ADMIN.NGUOI_DUNG.BASE_PATH,
          ]);
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
  }

  openForgotPassword(): void {
    this.dialogService.success({
      title: 'Quên mật khẩu',
      message:
        'Vui lòng liên hệ quản trị viên để được cấp lại mật khẩu.',
      closeButtonText: 'Đóng',
      width: '420px',
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}
