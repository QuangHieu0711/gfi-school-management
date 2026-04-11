/* eslint-disable @typescript-eslint/no-explicit-any */
import { CommonModule } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { ID_TYPE } from '@model/response.model';

import { MenuResponse } from '@app/model/admin/menu.model';
import { VaiTroResponse } from '@app/model/admin/vai-tro.model';
import { MenuService } from '@app/service/admin/menu.service';
import { VaiTroService } from '@app/service/admin/vai-tro.service';
import { PhanQuyenChucNangComponent } from '../phan-quyen-chuc-nang/phan-quyen-chuc-nang.component';
import { PhanQuyenDuLieuComponent } from '../phan-quyen-du-lieu/phan-quyen-du-lieu.component';

@Component({
  selector: 'cau-hinh-vai-tro',
  templateUrl: './cau-hinh-vai-tro.component.html',
  styleUrls: ['./cau-hinh-vai-tro.component.scss'],
  imports: [
    CommonModule,
    RouterLink,
    IconComponent,
    PhanQuyenChucNangComponent,
    PhanQuyenDuLieuComponent,
    ...MATERIAL_MODULE,
  ],
})
export class CauHinhVaiTroComponent extends ComponentBaseAbstract {
  activeTab: 'function' | 'data' = 'function';
  roleId: ID_TYPE | null = null;
  roleDetail?: VaiTroResponse;
  menusSnapshot: MenuResponse[] = [];
  baseLoading = false;
  hasDirtyChanges = false;
  dirtyChangeCount = 0;

  constructor(
    protected override injector: Injector,
    private readonly activatedRoute: ActivatedRoute,
    private readonly vaiTroService: VaiTroService,
    private readonly menuService: MenuService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.roleId = this.activatedRoute.snapshot.paramMap.get('id');
    const tabParam = this.activatedRoute.snapshot.queryParamMap.get('tab');

    // Default to 'function' tab, allow override from URL
    this.activeTab = tabParam === 'data' ? 'data' : 'function';

    if (this.roleId == null) {
      this.toastr.error('Không tìm thấy vai trò cần cấu hình', 'Thất bại');
      return;
    }

    this.loadInitialSnapshot();
  }

  setActiveTab(index: number) {
    this.activeTab = index === 0 ? 'function' : 'data';
    // Update URL query parameter
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { tab: this.activeTab },
      queryParamsHandling: 'merge',
    });
  }

  onDirtyStateChange(state: { count: number; hasDirty: boolean }) {
    this.hasDirtyChanges = state.hasDirty;
    this.dirtyChangeCount = state.count;
  }

  resetChanges() {
    // Will be handled by child component
  }

  saveChanges() {
    // Will be handled by child component
  }

  getRoleName(): string {
    return (
      this.roleDetail?.roleName ??
      this.roleDetail?.code ??
      `Vai trò #${this.roleId}`
    );
  }

  private loadInitialSnapshot() {
    if (this.roleId == null) return;

    this.baseLoading = true;
    forkJoin({
      role: this.vaiTroService.getById(this.roleId),
      menus: this.menuService.filter({}),
    })
      .pipe(finalize(() => (this.baseLoading = false)))
      .subscribe({
        next: ({ role, menus }) => {
          this.roleDetail = role.data;
          this.menusSnapshot = this.normalizeMenus(menus.data ?? []);
        },
        error: (error) => {
          this.toastr.error(
            error?.error?.userMessage ??
              error?.error?.message ??
              'Không tải được dữ liệu cấu hình vai trò',
            'Thất bại'
          );
        },
      });
  }

  private normalizeMenus(items: any[]): MenuResponse[] {
    const normalized = (items ?? []).map((raw) => ({
      ...raw,
      id: raw.menuId ?? raw.id,
      name: raw.menuName ?? raw.name,
      code: raw.menuCode ?? raw.code,
      url: raw.menuUrl ?? raw.url,
      icon: raw.icon ?? null,
      ordinal: raw.ordinal ?? 0,
      parentCode: raw.parentCode ?? raw.menuParentCode ?? null,
      parentId: raw.parentId ?? raw.parentMenuId ?? null,
    }));

    const codeToId = new Map(
      normalized
        .filter((item) => item.code != null)
        .map((item) => [item.code, item.id])
    );

    return normalized.map((item) => ({
      ...item,
      parentId:
        item.parentId ??
        (item.parentCode ? (codeToId.get(item.parentCode) ?? null) : null),
    }));
  }
}
