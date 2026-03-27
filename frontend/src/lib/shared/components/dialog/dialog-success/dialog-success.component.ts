import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogData } from '@model/dialog.model';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-dialog-success',
  templateUrl: './dialog-success.component.html',
  imports: [...MATERIAL_MODULE],
})
export class DialogSuccessComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<DialogSuccessComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {}

  onClose() {
    this.dialogRef.close();
  }
}
