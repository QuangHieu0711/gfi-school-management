/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatDialog } from '@angular/material/dialog';
import { ICurrentUser } from '@model/auth.model';
import { AuthService } from '@service';
import { MATERIAL_MODULE } from '@modules';
interface UserDisplayInfo {
  username: string;
  email: string;
  name: string;
  maDonVi: string;
  tenDonVi: string;
  role: string;
}

@Component({
  selector: 'app-user-profile-dialog',
  templateUrl: './user-profile-dialog.component.html',
  styleUrls: ['./user-profile-dialog.component.scss'],
  standalone: true,
  imports: [CommonModule, ...MATERIAL_MODULE],
})
export class UserProfileDialogComponent {
  title = 'THÔNG TIN TÀI KHOẢN';
  isLoading = true;

  userDisplayInfo: UserDisplayInfo = {
    username: 'N/A',
    email: 'N/A',
    name: 'N/A',
    maDonVi: 'N/A',
    tenDonVi: 'N/A',
    role: 'N/A',
  };

  constructor(
    private authService: AuthService,
    public dialogRef: MatDialogRef<UserProfileDialogComponent>,
    private dialog: MatDialog,
    @Inject(MAT_DIALOG_DATA) public userData: ICurrentUser | null
  ) {
    this.loadUserFromApi();
  }

  // openChangePassword(): void {
  //   const changeRef = this.dialog.open(ChangePasswordDialogComponent, {
  //     width: '480px',
  //     data: { username: this.userDisplayInfo.username },
  //   });

  //   changeRef.afterClosed().subscribe((result) => {
  //     if (result && result.saved) {
  //       this.dialog.open(UserProfileDialogComponent, {
  //         width: '600px',
  //         data: this.userData,
  //       });
  //     }
  //   });

  //   this.dialogRef.close();
  // }

  private loadUserFromApi(): void {
    this.isLoading = true;

    this.authService.getCurrentUser().subscribe({
      next: (user: any) => {
        if (user && user.id) {
          this.mapUserToDisplayInfo(user);
        } else {
          this.mapUserToDisplayInfo(this.userData);
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Lỗi khi tải thông tin người dùng:', err);
        this.mapUserToDisplayInfo(this.userData);
        this.isLoading = false;
      },
    });
  }

  private mapUserToDisplayInfo(user: any): void {
    if (!user) {
      this.resetToDefault();
      return;
    }

    this.userDisplayInfo = {
      username: user.username || 'N/A',
      email: user.email || 'N/A',
      name: user.fullName || 'N/A',
      maDonVi: user.unit?.code || 'N/A',
      tenDonVi: user.unit?.name || 'N/A',
      role: user.role?.name || 'N/A',
    };
  }

  private resetToDefault(): void {
    Object.assign(this.userDisplayInfo, {
      username: 'N/A',
      email: 'N/A',
      name: 'N/A',
      maDonVi: 'N/A',
      tenDonVi: 'N/A',
      role: 'N/A',
    });
  }
}
