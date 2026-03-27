import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface IRule {
  ruleId: number;
  roleId: number;
  moduleId: number;
  isView: number;
  isAdd: number;
  isEdit: number;
  isEditMap: number;
  isViewMap: number;
  isDelete: number;
  isDownload: number;
  isApprove: number;
  name?: string;
  url?: string;
  pid?: number;
  pathId?: string;
  ordinal?: number;
  icon?: string;
  categoryId?: string;
  module?: string;
  idKhumo?: string;
}

export interface IScreenPermission {
  canView: boolean;
  canAdd: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canApprove: boolean;
  canDownload: boolean;
  canEditMap: boolean;
  canViewMap: boolean;
}

export interface IPermissionParams {
  pathSegment: string;
  idLoaiTaiLieu?: string;
  idKhuMo?: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotAdminPermissionService {
  private readonly USER_INFO_KEY = 'userInfo';

  private rulesSubject = new BehaviorSubject<IRule[]>([]);
  public rules$: Observable<IRule[]> = this.rulesSubject.asObservable();

  private readonly MODULE_MAP: Record<string, string> = {
    'tai-lieu-nguyen-thuy': 'TLNT',
    'quan-ly-san-xuat': 'QLSX',
    'bao-cao-dia-chat': 'BCDC',
    'quan-ly-tai-nguyen': 'QLTN',
    'du-an-khai-thac': 'DAKT',
  };

  constructor() {
    this.initDataFromStorage();
  }

  private initDataFromStorage(): void {
    const userInfoRaw = localStorage.getItem(this.USER_INFO_KEY);

    if (userInfoRaw) {
      try {
        const userInfo = JSON.parse(userInfoRaw);
        if (userInfo.role && userInfo.role.rules) {
          this.rulesSubject.next(userInfo.role.rules as IRule[]);
        }
      } catch (e) {
        console.error('Lỗi parse userInfo từ LocalStorage:', e);
      }
    }
  }
  getMatchedRule(
    module: string,
    idKhuMo?: string,
    categoryId?: string
  ): IRule | undefined {
    const currentRules = this.rulesSubject.value;
    if (!currentRules.length) return undefined;
    const specificRule = currentRules.find(
      (r) =>
        r.module === module &&
        r.idKhumo === idKhuMo &&
        r.categoryId === categoryId
    );

    if (specificRule) return specificRule;

    return currentRules.find(
      (r) =>
        r.module === module &&
        r.idKhumo === idKhuMo &&
        (!r.categoryId || r.categoryId === '')
    );
  }

  hasScreenAccess(params: IPermissionParams): IScreenPermission {
    const { pathSegment, idLoaiTaiLieu, idKhuMo } = params;
    const moduleCode = this.MODULE_MAP[pathSegment] || pathSegment;
    const categoryId = idLoaiTaiLieu;
    const rule = this.getMatchedRule(moduleCode, idKhuMo, categoryId);

    if (!rule) {
      return this.getEmptyPermission();
    }

    return {
      canView: rule.isView === 1,
      canAdd: rule.isAdd === 1,
      canEdit: rule.isEdit === 1,
      canDelete: rule.isDelete === 1,
      canApprove: rule.isApprove === 1,
      canDownload: rule.isDownload === 1,
      canEditMap: rule.isEditMap === 1,
      canViewMap: rule.isViewMap === 1,
    };
  }

  private getEmptyPermission(): IScreenPermission {
    return {
      canView: false,
      canAdd: false,
      canEdit: false,
      canDelete: false,
      canApprove: false,
      canDownload: false,
      canEditMap: false,
      canViewMap: false,
    };
  }
}
