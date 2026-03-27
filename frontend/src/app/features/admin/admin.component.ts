import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ComponentBaseAbstract } from '@layout';
import { NavigatorAction } from '@store/navigator';
import { MenuItem, Rule } from './admin.interface';
import { AuthService } from '@service';

@Component({
  selector: 'admin',
  templateUrl: './admin.component.html',
  imports: [RouterOutlet],
})
export class AdminComponent extends ComponentBaseAbstract {
  private authService = inject(AuthService);

  protected override componentInit(): void {
    this.loadDynamicMenu();
  }

  private loadDynamicMenu() {
    const currentUser = this.authService.currentUser;

    const rules: Rule[] = (currentUser?.role?.rules || []) as unknown as Rule[];

    const viewableRules = rules.filter((r) => r.isView === 1);

    viewableRules.sort((a, b) => (a.ordinal || 0) - (b.ordinal || 0));

    const dynamicMenu = this.buildMenuTree(viewableRules);

    this.store.dispatch(
      NavigatorAction.Update({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        newState: dynamicMenu as any,
      })
    );
  }

  private buildMenuTree(rules: Rule[]): MenuItem[] {
    const map = new Map<number, MenuItem>();
    const roots: MenuItem[] = [];

    rules.forEach((rule) => {
      if (rule.moduleId === 0) {
        return;
      }
      const cleanName = rule.name?.trim();
      let cleanUrl = rule.url?.trim();
      const cleanIcon = rule.icon?.trim() || 'timeline';

      if (cleanUrl && cleanUrl !== '#' && !cleanUrl.startsWith('http')) {
        if (!cleanUrl.startsWith('/')) {
          cleanUrl = [
            '/',
            this.navigatorEndpoint.ADMIN.BASE_PATH,
            cleanUrl,
          ].join('/');
        }
      } else {
        cleanUrl = '';
      }

      map.set(rule.moduleId, {
        key: rule.moduleId.toString(),
        name: cleanName,
        icon: cleanIcon,
        url: cleanUrl,
        children: [],
        expanded: true,
      } as MenuItem);
    });

    // rules.forEach((rule) => {
    //   const node = map.get(rule.moduleId);
    //   if (node) {
    //     if (rule.pid && map.has(rule.pid)) {
    //       const parent = map.get(rule.pid);
    //       parent?.children?.push(node);
    //     } else {
    //       roots.push(node);
    //     }
    //   }
    //   // });
    // });
    rules.forEach((rule) => {
      const node = map.get(rule.moduleId);
      if (node) {
        if (rule.pid && map.has(rule.pid)) {
          const parent = map.get(rule.pid);
          parent?.children?.push(node);
        } else {
          roots.push(node);
        }
      }
    });
    map.forEach((node) => {
      if (node.children && node.children.length > 0) {
        node.url = '';
        node.expanded = true;
      }
    });

    return roots;
  }
}
