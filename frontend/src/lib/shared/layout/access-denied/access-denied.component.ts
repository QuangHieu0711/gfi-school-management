import { CommonModule, Location } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './access-denied.component.html',
  styleUrls: ['./access-denied.component.scss'],
})
export class AccessDeniedComponent {
  constructor(
    private readonly router: Router,
    private readonly location: Location
  ) {}

  goHome(): void {
    this.router.navigate(['/']);
  }

  goLogin(): void {
    this.router.navigate(['/login']);
  }

  goBack(): void {
    this.location.back();
  }
}
