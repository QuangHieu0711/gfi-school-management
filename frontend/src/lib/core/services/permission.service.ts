import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { IRule } from '@model/auth.model';

@Injectable({
  providedIn: 'root',
})
export class PermissionService {
  private rulesSubject = new BehaviorSubject<IRule[]>([]);
  rules$ = this.rulesSubject.asObservable();

  private normalizeUrl(url?: string): string {
    return (url ?? '').trim().toLowerCase().replace(/^\/+/, '');
  }

  setRules(rules: IRule[]) {
    const normalized = rules.map((r) => ({
      ...r,
      url: this.normalizeUrl(r.url),
    }));

    this.rulesSubject.next(normalized);
  }

  get rules(): IRule[] {
    return this.rulesSubject.value;
  }

  has(moduleUrl: string, action: keyof IRule): boolean {
    const target = this.normalizeUrl(moduleUrl);
    if (!target) return false;

    const candidates = new Set<string>([target]);

    if (target.startsWith('admin/')) {
      candidates.add(target.replace(/^admin\//, ''));
    } else {
      candidates.add(`admin/${target}`);
    }

    let rule: IRule | undefined;
    for (const item of this.rules) {
      if (candidates.has(this.normalizeUrl(item.url))) {
        rule = item;
        break;
      }
    }

    if (!rule) return false;

    return rule[action] === 1;
  }

  canView(url: string) {
    return this.has(url, 'isView');
  }

  canAdd(url: string) {
    return this.has(url, 'isAdd');
  }

  canEdit(url: string) {
    return this.has(url, 'isEdit');
  }

  canDelete(url: string) {
    return this.has(url, 'isDelete');
  }

  canConfig(url: string) {
    return this.has(url, 'isConfig');
  }
}
