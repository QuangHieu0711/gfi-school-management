import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogData } from '@model/dialog.model';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-dialog-confirm',
  templateUrl: './dialog-confirm.component.html',
  imports: [...MATERIAL_MODULE],
})
export class DialogConfirmComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<DialogConfirmComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {}

  onCancel() {
    this.dialogRef.close(false);
  }
  onConfirm() {
    this.dialogRef.close(true);
  }
}
