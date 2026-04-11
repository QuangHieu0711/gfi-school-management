import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type PermissionAction =
  | 'view'
  | 'add'
  | 'edit'
  | 'delete'
  | 'download'
  | 'config';

export interface UserPermission {
  id?: string | number | null;
  roleId?: string | number;
  menuId?: string | number;
  menuCode: string;
  menuName?: string | null;
  menuUrl?: string | null;
  parentId?: string | number | null;
  icon?: string | null;
  ordinal?: number | null;
  isView: number;
  isAdd: number;
  isEdit: number;
  isDelete: number;
  isDownload: number;
  isConfig: number;
  dataScopes?: Array<{
    scopeType?: string;
    scopeValues?: number[];
  }>;
}

const PERMISSION_STORAGE_KEY = 'user.permissions';

@Injectable({
  providedIn: 'root',
})
export class PermissionCheckService {
  private readonly permissionsSubject = new BehaviorSubject<UserPermission[]>(
    this.readFromStorage()
  );

  readonly permissions$ = this.permissionsSubject.asObservable();

  setPermissions(permissions: UserPermission[]): void {
    const normalized = (permissions ?? []).map((item) => ({
      ...item,
      menuCode: this.normalizeMenuCode(item.menuCode),
    }));

    this.permissionsSubject.next(normalized);
    sessionStorage.setItem(PERMISSION_STORAGE_KEY, JSON.stringify(normalized));
  }

  getPermissions(): UserPermission[] {
    return this.permissionsSubject.value;
  }

  clearPermissions(): void {
    this.permissionsSubject.next([]);
    sessionStorage.removeItem(PERMISSION_STORAGE_KEY);
  }

  can(menuCode: string, action: PermissionAction): boolean {
    const permission = this.findPermission(menuCode);
    if (!permission) return false;

    switch (action) {
      case 'view':
        return permission.isView === 1;
      case 'add':
        return permission.isAdd === 1;
      case 'edit':
        return permission.isEdit === 1;
      case 'delete':
        return permission.isDelete === 1;
      case 'download':
        return permission.isDownload === 1;
      case 'config':
        return permission.isConfig === 1;
      default:
        return false;
    }
  }

  canView(menuCode: string): boolean {
    return this.can(menuCode, 'view');
  }

  canAdd(menuCode: string): boolean {
    return this.can(menuCode, 'add');
  }

  canEdit(menuCode: string): boolean {
    return this.can(menuCode, 'edit');
  }

  canDelete(menuCode: string): boolean {
    return this.can(menuCode, 'delete');
  }

  canDownload(menuCode: string): boolean {
    return this.can(menuCode, 'download');
  }

  canConfig(menuCode: string): boolean {
    return this.can(menuCode, 'config');
  }

  /**
   * Get allowed scope values for a specific menu code and scope type
   * @param menuCode The menu code to check
   * @param scopeType The scope type (UNIT, GRADE, CLASS)
   * @returns Array of allowed scope values
   */
  getAllowedScopeValues(
    menuCode: string,
    scopeType: 'UNIT' | 'GRADE' | 'CLASS'
  ): number[] {
    return this.getDataScopes(menuCode)
      .filter((x) => (x.scopeType ?? '').toUpperCase() === scopeType)
      .flatMap((x) => x.scopeValues ?? []);
  }

  /**
   * Check if a menu code has a specific scope type
   * @param menuCode The menu code to check
   * @param scopeType The scope type to verify (ALL, SELF, UNIT, GRADE, CLASS)
   * @returns True if the menu has the specified scope
   */
  hasScope(
    menuCode: string,
    scopeType: 'ALL' | 'SELF' | 'UNIT' | 'GRADE' | 'CLASS'
  ): boolean {
    return this.getDataScopes(menuCode).some(
      (x) => (x.scopeType ?? '').toUpperCase() === scopeType
    );
  }

  /**
   * Get all data scopes for a menu code
   * @param menuCode The menu code to check
   * @returns Array of data scopes
   */
  private getDataScopes(
    menuCode: string
  ): Array<{ scopeType?: string; scopeValues?: number[] }> {
    const permission = this.findPermission(menuCode);
    return permission?.dataScopes ?? [];
  }

  private findPermission(menuCode: string): UserPermission | undefined {
    const normalizedCode = this.normalizeMenuCode(menuCode);
    return this.permissionsSubject.value.find(
      (item) => item.menuCode === normalizedCode
    );
  }

  private normalizeMenuCode(menuCode?: string): string {
    return (menuCode ?? '').trim().toUpperCase();
  }

  private readFromStorage(): UserPermission[] {
    try {
      const raw = sessionStorage.getItem(PERMISSION_STORAGE_KEY);
      const parsed = raw ? (JSON.parse(raw) as UserPermission[]) : [];
      return (parsed ?? []).map((item) => ({
        ...item,
        menuCode: this.normalizeMenuCode(item.menuCode),
      }));
    } catch {
      return [];
    }
  }
}
