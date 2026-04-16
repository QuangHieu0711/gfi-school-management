// /* eslint-disable @typescript-eslint/no-explicit-any */
// import { Component, Injector } from '@angular/core';
// import { RouterModule, RouterOutlet } from '@angular/router';
// import { ISidebarItem } from '@model/menu.model';
// import {
//   ComponentBaseAbstract,
//   HeaderComponent,
//   NavigatorCollapsedComponent,
// } from '@layout';
// import { MATERIAL_MODULE } from '@modules';
// import { TreeComponent } from '@components/app-tree/app-tree.component';
// import { TreeNode } from '@model/tree.models';
// import { UserInfoAction } from '@store/user-info';
// import { distinctUntilChanged, filter, tap } from 'rxjs';
// import { IconComponent } from '@components/app-icon/app-icon.component';
// import { MENU } from '@constant/menu';
// import { getObsValue } from '@utils/utils';
// import { ICurrentUser, UserRole } from '@model/auth.model';

// @Component({
//   selector: 'app-layout-component',
//   templateUrl: './layout.component.html',
//   styleUrls: ['./layout.component.scss'],
//   imports: [
//     RouterOutlet,
//     // Material
//     ...MATERIAL_MODULE,
//     // Component
//     HeaderComponent,
//     NavigatorCollapsedComponent,
//     TreeComponent,
//     RouterModule,
//     IconComponent,
//   ],
// })
// export class LayoutComponent extends ComponentBaseAbstract {
//   isExpanded = true;
//   menu: ISidebarItem[] = [];
//   treeDataSource: TreeNode[] = [];
//   userInfo = getObsValue(this.store.select((state) => state.userInfo));

//   constructor(protected override injector: Injector) {
//     super(injector);
//   }

//   protected override componentInit(): void {
//     this.getUserInfo();
//     this.getnavigator();
//   }

//   getnavigator(): void {
//     this.store
//       .select((state) => state.navigator)
//       .subscribe((navigator) => {
//         this.treeDataSource = [];
//         setTimeout(() => {
//           this.treeDataSource = navigator;
//         });
//       });
//   }

//   getUserInfo(): void {
//     this.store
//       .select((state) => state.userInfo)
//       .pipe(
//         tap((u) => {
//           if (!u?.id) this.store.dispatch(UserInfoAction.GetCurrentUser());
//         }),
//         filter((u) => !!u?.id),
//         distinctUntilChanged((a, b) => a.id === b.id)
//       )
//       .subscribe((userInfo) => {
//         this.getMenuByRole(userInfo);
//       });
//   }

//   goToUrl(url: string): void {
//     // support either a plain url string or a node object with `url` and `queryParams`
//     const target: any = url as any;
//     if (!target) return;

//     if (typeof target === 'string') {
//       this.router.navigateByUrl(target);
//       return;
//     }

//     const nodeUrl: string = target.url ?? target['url'] ?? '';
//     const queryParams = target.queryParams ?? target['queryParams'];

//     if (queryParams && nodeUrl) {
//       const commands = nodeUrl.split('/').filter((p: string) => p !== '');
//       // ensure leading slash navigation
//       this.router.navigate(['/', ...commands], { queryParams });
//       return;
//     }

//     if (nodeUrl) {
//       this.router.navigateByUrl(nodeUrl);
//       return;
//     }

//     // fallback: try navigating to root
//     this.router.navigateByUrl('/');
//   }

//   /**
//    * Helper to check whether a node with a specific url and queryParams is active.
//    * Keeps template expressions simple by accepting url and queryParams separately.
//    */
//   isNodeActiveUrl(
//     url?: string | null,
//     queryParams?: Record<string, unknown> | null
//   ): boolean {
//     if (!url) return false;
//     return this.isPathActive({
//       url,
//       queryParams: queryParams ?? undefined,
//     } as any);
//   }

//   getMenuByRole(userInfo: ICurrentUser): void {
//     this.menu = this.resolveMenu(MENU, this.navigatorEndpoint).filter(
//       (menuItem) =>
//         userInfo.role.rules.some(
//           (rule) => rule.url === menuItem.url && rule.isView === 1
//         ) ||
//         menuItem.key === 'dashboard' ||
//         menuItem.key === 'baoCaoDiaChat' ||
//         menuItem.key === 'deAnPhuongAn' ||
//         menuItem.key === 'taiLieuNguyenThuy' ||
//         menuItem.key === 'quanTriTaiNguyen' || // TODO: xÃ³a
//         (menuItem.key === 'admin' && userInfo.role.name === UserRole.ADMIN)
//     );
//   }
// }

/* eslint-disable @typescript-eslint/no-explicit-any */
import { NavigationEnd } from '@angular/router';
import { Subject, distinctUntilChanged, filter, tap, takeUntil } from 'rxjs';
import { Component, Injector } from '@angular/core';
import { RouterModule, RouterOutlet } from '@angular/router';
import { ISidebarItem } from '@model/menu.model';
import { ComponentBaseAbstract, HeaderComponent } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { TreeComponent } from '@components/app-tree/app-tree.component';
import { TreeNode } from '@model/tree.models';
import { UserInfoAction } from '@store/user-info';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { MENU } from '@constant/menu';
import { getObsValue } from '@utils/utils';
import { ICurrentUser, IMenuPermission, UserRole } from '@model/auth.model';

@Component({
  selector: 'app-layout-component',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss'],
  imports: [
    RouterOutlet,
    ...MATERIAL_MODULE,
    HeaderComponent,
    TreeComponent,
    RouterModule,
    IconComponent,
  ],
})
export class LayoutComponent extends ComponentBaseAbstract {
  Number = Number; // Expose global Number for template
  private readonly SIDEBAR_COLLAPSED_KEY = 'layout.sidebar.collapsed';
  isExpanded = true;
  isSidebarCollapsed = false;
  sidebarSearchTerm = '';
  menu: ISidebarItem[] = [];
  treeDataSource: TreeNode[] = [];
  private allTreeDataSource: TreeNode[] = [];
  private isPermissionDrivenSidebar = false;
  userInfo = getObsValue(this.store.select((state) => state.userInfo));

  private readonly TREE_MODULE_PREFIXES = ['/Admin'];
  private lastTopPath = '';
  private readonly destroy$ = new Subject<void>();

  constructor(protected override injector: Injector) {
    super(injector);
  }

  protected override componentInit(): void {
    this.restoreSidebarState();
    this.getUserInfo();
    this.getnavigator();
    this.bindSidebarLifecycleByRoute();
    this.bindChangeDetectionOnNavigate();
  }
  protected override componentDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.clearSidebar();
  }

  private bindSidebarLifecycleByRoute(): void {
    this.applySidebarByUrl(this.router.url);

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        tap((e) => this.applySidebarByUrl(e.urlAfterRedirects)),
        takeUntil(this.destroy$)
      )
      .subscribe();
  }
  private applySidebarByUrl(rawUrl: string): void {
    const pathOnly = (rawUrl || '').split('?')[0] || '/';
    const topPath = '/' + (pathOnly.split('/')[1] || '');

    // =========================
    // DASHBOARD (Trang chá»§)
    // =========================
    if (pathOnly === '/' || pathOnly === '') {
      this.isExpanded = false; // thu gá»n sidebar
      this.clearSidebar();
      return;
    }

    // cÃ¡c module cÃ³ tree
    const isTreeModule = this.TREE_MODULE_PREFIXES.some((p) =>
      pathOnly.startsWith(p)
    );

    // if (topPath !== this.lastTopPath) {
    //   this.lastTopPath = topPath;
    //   this.clearSidebar();
    // }
    if (topPath !== this.lastTopPath) {
      this.lastTopPath = topPath;

      // chá»‰ collapse sidebar
      // this.isExpanded = false;
    }

    if (!isTreeModule) {
      this.clearSidebar();
      return;
    }

    // náº¿u lÃ  module khÃ¡c dashboard thÃ¬ má»Ÿ sidebar
    this.isExpanded = true;
  }

  // private clearSidebar(): void {
  //   this.treeDataSource = [];
  // }
  private clearSidebar(): void {
    // khÃ´ng xÃ³a dá»¯ liá»‡u navigator
    // chá»‰ Ä‘á»ƒ UI tá»± xá»­ lÃ½ expand/collapse
  }

  toggleSidebarCollapsed(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
    localStorage.setItem(
      this.SIDEBAR_COLLAPSED_KEY,
      String(this.isSidebarCollapsed)
    );
    this.cdRef.markForCheck();
  }

  private restoreSidebarState(): void {
    this.isSidebarCollapsed =
      localStorage.getItem(this.SIDEBAR_COLLAPSED_KEY) === 'true';
  }

  getnavigator(): void {
    this.store
      .select((state) => state.navigator)
      .pipe(
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntil(this.destroy$)
      )
      .subscribe((navigator) => {
        // Only use navigator if permissions-driven sidebar is not active
        if (this.isPermissionDrivenSidebar) {
          return;
        }
        this.allTreeDataSource =
          navigator && navigator.length ? [...navigator] : [];
        this.applySidebarSearch();
      });
  }

  private bindChangeDetectionOnNavigate(): void {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.cdRef.markForCheck();
      });
  }
  getUserInfo(): void {
    this.store
      .select((state) => state.userInfo)
      .pipe(
        tap((u) => {
          if (!u?.id) {
            this.store.dispatch(UserInfoAction.GetCurrentUser());
          }
        }),
        filter((u) => !!u?.id),
        distinctUntilChanged((a, b) => a.id === b.id),
        takeUntil(this.destroy$)
      )
      .subscribe((userInfo) => {
        this.getMenuByRole(userInfo);
        this.syncSidebarFromPermissions(userInfo.permissions?.menus ?? []);
      });
  }

  goToUrl(url: string): void {
    const target: any = url;

    if (!target) return;
    const menuName = target?.name ?? target?.label ?? target?.title;
    const encodedName = encodeURIComponent(menuName);
    if (typeof target === 'string') {
      this.router.navigateByUrl(`${target}?muc=${encodedName}`);
      return;
    }
    const nodeUrl: string = target.url ?? target['url'] ?? '';

    const baseQuery = target.queryParams ?? target['queryParams'] ?? {};
    const queryParams = { ...baseQuery, muc: menuName };

    if (queryParams && nodeUrl) {
      const commands = nodeUrl.split('/').filter((p: string) => p !== '');
      this.router.navigate(['/', ...commands], { queryParams });
      return;
    }

    if (nodeUrl) {
      this.router.navigateByUrl(nodeUrl);
      return;
    }

    this.router.navigateByUrl('/');
  }

  isNodeActiveUrl(url: string, queryParams?: Record<string, any>): boolean {
    if (!url) return false;

    const targetPath = url.startsWith('/') ? url : `/${url}`;

    // ===== default behavior =====
    const pathOk = this.router.isActive(targetPath, {
      paths: 'subset',
      queryParams: 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored',
    });

    if (!pathOk) return false;

    if (queryParams && Object.keys(queryParams).length > 0) {
      const currentQ = this.route.snapshot.queryParams || {};
      return Object.keys(queryParams).every((k) => {
        const expected = String(queryParams[k] ?? '');
        const actual = String(currentQ[k] ?? '');
        return expected === actual;
      });
    }

    return true;
  }

  isTreeNodeActive(node: Record<string, any> | null | undefined): boolean {
    if (!node) return false;

    const nodeUrl = node['url'];
    const nodeQueryParams = node['activeQueryParams'] ?? node['queryParams'];

    if (
      typeof nodeUrl === 'string' &&
      this.isNodeActiveUrl(nodeUrl, nodeQueryParams)
    ) {
      return true;
    }

    const children = Array.isArray(node['children']) ? node['children'] : [];
    return children.some((child) => this.isTreeNodeActive(child));
  }

  isTreeNodeCurrent(node: Record<string, any> | null | undefined): boolean {
    if (!node) return false;

    const nodeUrl = node['url'];
    const nodeQueryParams = node['activeQueryParams'] ?? node['queryParams'];

    return (
      typeof nodeUrl === 'string' &&
      this.isNodeActiveUrl(nodeUrl, nodeQueryParams)
    );
  }

  getMenuByRole(userInfo: ICurrentUser): void {
    this.menu = this.resolveMenu(MENU, this.navigatorEndpoint).filter(
      (menuItem) =>
        menuItem.key === 'admin' && userInfo.role.name === UserRole.ADMIN
    );
  }

  onSidebarSearch(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    this.sidebarSearchTerm = target?.value ?? '';
    this.applySidebarSearch();
  }

  private applySidebarSearch(): void {
    const keyword = this.sidebarSearchTerm.trim().toLowerCase();
    if (!keyword) {
      this.treeDataSource = [...this.allTreeDataSource];
      return;
    }

    this.treeDataSource = this.filterTreeByKeyword(
      this.allTreeDataSource,
      keyword
    );
  }

  private filterTreeByKeyword(nodes: TreeNode[], keyword: string): TreeNode[] {
    return nodes
      .map((node) => {
        const children = Array.isArray(node.children) ? node.children : [];
        const filteredChildren = this.filterTreeByKeyword(children, keyword);
        const selfMatched = `${node.name ?? ''}`
          .toLowerCase()
          .includes(keyword);

        if (!selfMatched && filteredChildren.length === 0) {
          return null;
        }

        return {
          ...node,
          expanded: true,
          children: filteredChildren,
        } as TreeNode;
      })
      .filter((node): node is TreeNode => node !== null);
  }

  private syncSidebarFromPermissions(menus: IMenuPermission[]): void {
    if (!menus.length) {
      this.isPermissionDrivenSidebar = false;
      this.allTreeDataSource = [];
      this.treeDataSource = [];
      return;
    }

    this.isPermissionDrivenSidebar = true;
    this.allTreeDataSource = this.buildSidebarTreeFromMenus(menus);
    this.applySidebarSearch();
  }

  private buildSidebarTreeFromMenus(menus: IMenuPermission[]): TreeNode[] {
    const mapNode = (
      menu: IMenuPermission,
      ordinalFallback: number,
      level = 0
    ): TreeNode | null => {
      // Process children first
      const children = (menu.children ?? [])
        .map((child, index) => mapNode(child, index, level + 1))
        .filter((node): node is TreeNode => node !== null);

      const parentCanView = Number(menu.actions?.isView ?? 0) === 1;

      // Skip if no view permission AND no children to display
      if (!parentCanView && children.length === 0) {
        return null;
      }

      const url =
        typeof menu.path === 'string' && menu.path.trim() !== ''
          ? this.ensureLeadingSlash(menu.path)
          : null;

      // Skip if no URL and no children
      if (!url && children.length === 0) {
        return null;
      }

      return {
        id: menu.menuCode,
        parentId: menu.parentMenuId ?? null,
        name: menu.menuName ?? this.getMenuLabel(menu.menuCode),
        url: parentCanView ? url : null, // Only set URL if has view permission
        icon: menu.icon || 'menu',
        ordinal: Number(menu.level ?? ordinalFallback),
        level,
        expanded: children.length > 0,
        children,
      };
    };

    return this.sortTreeNodes(
      menus
        .map((menu, index) => mapNode(menu, index, 0))
        .filter((node): node is TreeNode => node !== null)
    );
  }

  private sortTreeNodes(nodes: TreeNode[]): TreeNode[] {
    return [...nodes]
      .sort(
        (a, b) =>
          Number(a['ordinal'] ?? 0) - Number(b['ordinal'] ?? 0) ||
          `${a.name ?? ''}`.localeCompare(`${b.name ?? ''}`)
      )
      .map((node) => {
        const children = this.sortTreeNodes(
          Array.isArray(node.children) ? node.children : []
        );

        return {
          ...node,
          level: Number(node['level'] ?? 0),
          expanded: children.length > 0,
          children,
        };
      });
  }

  private ensureLeadingSlash(url: string): string {
    return url.startsWith('/') ? url : `/${url}`;
  }

  private getMenuLabel(menuCode?: string): string {
    const map: Record<string, string> = {
      USER_ADMIN: 'Quáº£n trá»‹ ngÆ°á»i dÃ¹ng',
      ACCOUNT_MANAGEMENT: 'Quáº£n lÃ½ tÃ i khoáº£n',
      UNIT_MANAGEMENT: 'Quáº£n lÃ½ Ä‘Æ¡n vá»‹',
      SYSTEM_CONFIG: 'Cáº¥u hÃ¬nh há»‡ thá»‘ng',
      ROLE_MANAGEMENT: 'Quáº£n lÃ½ vai trÃ²',
      FUNCTION_MANAGEMENT: 'Quáº£n lÃ½ chá»©c nÄƒng',
      SCHOOL_YEAR_CONFIG: 'Cáº¥u hÃ¬nh nÄƒm há»c',
      GRADE_CONFIG: 'Cáº¥u hÃ¬nh khá»‘i',
      ACADEMIC_MANAGEMENT: 'Quáº£n lÃ½ há»c táº­p',
      CLASS_MANAGEMENT: 'Quáº£n lÃ½ lá»›p',
      SUBJECT_MANAGEMENT: 'Quáº£n lÃ½ mÃ´n há»c',
      STUDENT: 'Há»c sinh',
      STUDENT_PROFILE: 'Há»“ sÆ¡ há»c sinh',
      STAFF_PROFILE: 'Ho so can bo',
    };

    return map[menuCode ?? ''] ?? menuCode ?? '';
  }
}

