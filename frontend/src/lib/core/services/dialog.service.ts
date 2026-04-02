import { Injectable, inject } from '@angular/core';
import {
  MatDialog,
  MatDialogRef,
  MatDialogConfig,
} from '@angular/material/dialog';
import { ComponentType } from '@angular/cdk/portal';
import {
  DialogConfirmComponent,
  DialogErrorComponent,
  DialogSuccessComponent,
} from '@components/dialog';
import { DialogData } from '@model/dialog.model';
import { LanguageService } from './language.service';

@Injectable({ providedIn: 'root' })
export class DialogService {
  private readonly dialog = inject(MatDialog);
  private dialogRefs: MatDialogRef<unknown, unknown>[] = [];

  constructor(private readonly languageService: LanguageService) {}

  /**
   * Opens a component dialog and invokes callback on close (optional)
   * Strongly typed via component's `MatDialogRef` and `MAT_DIALOG_DATA` types.
   */
  componentDialog<T, D = unknown, R = unknown>(
    component: ComponentType<T>,
    config?: MatDialogConfig<D>,
    callback?: (result: R | undefined) => void
  ): MatDialogRef<T, R> {
    const dialogConfig: MatDialogConfig<D> = {
      ...config,
      width: config?.width ?? '500px',
    };
    const dialogRef = this.dialog.open<T, D, R>(component, {
      ...dialogConfig,
      disableClose: true,
      autoFocus: false,
    });

    if (callback) dialogRef.afterClosed().subscribe(callback);

    this.addDialogRef(dialogRef);
    return dialogRef;
  }

  /**
   * Confirm dialog (returns true/false)
   */
  confirm(
    payload: DialogData = this.getDefaultConfirmDialog(),
    callback?: (confirmed: boolean | undefined) => void
  ): MatDialogRef<DialogConfirmComponent, boolean> {
    const { width = '500px', ...rest }: DialogData = {
      ...this.getDefaultConfirmDialog(),
      ...payload,
    };
    const ref = this.dialog.open(DialogConfirmComponent, {
      width: width,
      disableClose: true,
      data: rest,
      autoFocus: 'button[matbutton="filled"]',
    });
    if (callback) ref.afterClosed().subscribe(callback);
    this.addDialogRef(ref);
    return ref;
  }

  /**
   * Success dialog (info message)
   */
  success(
    payload: DialogData = this.getDefaultSuccessDialog(),
    callback?: () => void
  ): MatDialogRef<DialogSuccessComponent> {
    const { width = '500px', ...rest }: DialogData = {
      ...this.getDefaultSuccessDialog(),
      ...payload,
    };
    const ref = this.dialog.open(DialogSuccessComponent, {
      width: width,
      data: { type: 'success', ...rest },
      autoFocus: false,
    });
    if (callback) ref.afterClosed().subscribe(() => callback());
    this.addDialogRef(ref);
    return ref;
  }

  /**
   * Error dialog
   */
  error(
    payload: DialogData = this.getDefaultErrorDialog(),
    callback?: () => void
  ): MatDialogRef<DialogErrorComponent> {
    const { width = '500px', ...rest }: DialogData = {
      ...this.getDefaultErrorDialog(),
      ...payload,
    };
    const ref = this.dialog.open(DialogErrorComponent, {
      width: width,
      data: { type: 'error', ...rest },
      autoFocus: false,
    });
    if (callback) ref.afterClosed().subscribe(() => callback());
    this.addDialogRef(ref);
    return ref;
  }

  /**
   * Track dialog refs and remove on close
   */
  private addDialogRef(dialogRef: MatDialogRef<unknown, unknown>): void {
    dialogRef.afterClosed().subscribe(() => {
      this.dialogRefs = this.dialogRefs.filter(
        (ref) => ref.id !== dialogRef.id
      );
    });
    this.dialogRefs.push(dialogRef);
  }

  /**
   * Close all tracked dialogs
   */
  closeAll(): void {
    this.dialogRefs.forEach((ref) => ref.close());
    this.dialogRefs = [];
  }

  /**
   * Get default confirm dialog data
   */
  private getDefaultConfirmDialog(): DialogData {
    return {
      title: this.languageService.instant('dialog.confirm.title'),
      message: this.languageService.instant('dialog.confirm.message'),
      width: '600px',
      cancelButtonText: this.languageService.instant('dialog.confirm.cancel'),
      confirmButtonText: this.languageService.instant('dialog.confirm.confirm'),
    };
  }

  /**
   * Get default success dialog data
   */
  private getDefaultSuccessDialog(): DialogData {
    return {
      title: this.languageService.instant('dialog.success.title'),
      message: this.languageService.instant('dialog.success.message'),
      width: '600px',
      closeButtonText: this.languageService.instant('dialog.success.close'),
    };
  }

  /**
   * Get default error dialog data
   */
  private getDefaultErrorDialog(): DialogData {
    return {
      title: this.languageService.instant('dialog.error.title'),
      message: this.languageService.instant('dialog.error.message'),
      width: '600px',
      closeButtonText: this.languageService.instant('dialog.error.close'),
    };
  }
}
