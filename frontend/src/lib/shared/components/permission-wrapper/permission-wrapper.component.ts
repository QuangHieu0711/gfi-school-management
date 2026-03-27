import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-permission-wrapper',
  standalone: true,
  imports: [],
  templateUrl: './permission-wrapper.component.html',
})
export class PermissionWrapperComponent {
  @Input({ required: true }) hasPermission!: boolean;

  constructor(public router: Router) {}
}
