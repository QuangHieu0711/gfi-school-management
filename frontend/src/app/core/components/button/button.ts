import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-button',
  imports: [
    ButtonModule
  ],
  templateUrl: './button.html',
  styleUrl: './button.scss',
  standalone: true
})
export class Button {
  @Output() clickEvent: EventEmitter<any> = new EventEmitter();

  typeColor = '';
  @Input() labelName = 'Button';
  @Input() disabledCtrl = false;
  @Input() isOnlyIcon = false;

  onClick(evt: any) {
    if (!this.disabledCtrl) {
      this.clickEvent.emit(evt);
    }
  }
}
