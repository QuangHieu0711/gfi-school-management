import {
  Directive,
  Input,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewContainerRef,
} from '@angular/core';
import { Subscription } from 'rxjs';

import { PermissionAction, PermissionCheckService } from '@service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true,
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  @Input('appHasPermission') menuCode = '';
  @Input('appHasPermissionAction') action: PermissionAction = 'view';

  private permissionSub?: Subscription;

  constructor(
    private readonly templateRef: TemplateRef<unknown>,
    private readonly viewContainer: ViewContainerRef,
    private readonly permissionCheckService: PermissionCheckService
  ) {}

  ngOnInit(): void {
    this.permissionSub = this.permissionCheckService.permissions$.subscribe(
      () => {
        this.render();
      }
    );
    this.render();
  }

  ngOnDestroy(): void {
    this.permissionSub?.unsubscribe();
  }

  private render(): void {
    const allowed = this.permissionCheckService.can(this.menuCode, this.action);
    this.viewContainer.clear();

    if (allowed) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}
