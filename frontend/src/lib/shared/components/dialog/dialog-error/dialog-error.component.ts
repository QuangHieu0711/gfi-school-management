import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogData } from '@model/dialog.model';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-dialog-error',
  templateUrl: './dialog-error.component.html',
  imports: [...MATERIAL_MODULE],
})
export class DialogErrorComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<DialogErrorComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {}

  onClose() {
    this.dialogRef.close();
  }
}
