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
//         menuItem.key === 'quanTriTaiNguyen' || // TODO: xóa
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
import { ICurrentUser, UserRole } from '@model/auth.model';

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
  private readonly SIDEBAR_COLLAPSED_KEY = 'layout.sidebar.collapsed';
  isExpanded = true;
  isSidebarCollapsed = false;
  menu: ISidebarItem[] = [];
  treeDataSource: TreeNode[] = [];
  userInfo = getObsValue(this.store.select((state) => state.userInfo));

  private readonly TREE_MODULE_PREFIXES = [
    '/Admin',
    '/tai-lieu-nguyen-thuy',
    '/bao-cao-dia-chat',
    '/de-an-phuong-an',
    '/dong-cua-mo',
    '/quan-tri-tai-nguyen',
  ];
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
    // DASHBOARD (Trang chủ)
    // =========================
    if (pathOnly === '/' || pathOnly === '') {
      this.isExpanded = false; // thu gọn sidebar
      this.clearSidebar();
      return;
    }

    // các module có tree
    const isTreeModule = this.TREE_MODULE_PREFIXES.some((p) =>
      pathOnly.startsWith(p)
    );

    // if (topPath !== this.lastTopPath) {
    //   this.lastTopPath = topPath;
    //   this.clearSidebar();
    // }
    if (topPath !== this.lastTopPath) {
      this.lastTopPath = topPath;

      // chỉ collapse sidebar
      // this.isExpanded = false;
    }

    if (!isTreeModule) {
      this.clearSidebar();
      return;
    }

    // nếu là module khác dashboard thì mở sidebar
    this.isExpanded = true;
  }

  // private clearSidebar(): void {
  //   this.treeDataSource = [];
  // }
  private clearSidebar(): void {
    // không xóa dữ liệu navigator
    // chỉ để UI tự xử lý expand/collapse
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
      // .subscribe((navigator) => {
      //   this.treeDataSource = navigator ?? [];
      // });
      .subscribe((navigator) => {
        this.treeDataSource =
          navigator && navigator.length ? [...navigator] : [];
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

    const currentPathOnly = this.router.url.split('?')[0];
    const targetPath = url.startsWith('/') ? url : `/${url}`;
    const currentQ = this.route.snapshot.queryParams || {};

    // =========================
    // TLNT rules
    // =========================
    const TLNT_ROOT = '/tai-lieu-nguyen-thuy';
    const TLNT_LANDING = '/tai-lieu-nguyen-thuy/vung-mo';

    if (targetPath === TLNT_ROOT) {
      //chỉ sáng khi đúng landing và chưa chọn vùng
      return currentPathOnly === TLNT_LANDING && !currentQ['idVungMo'];
    }

    if (
      targetPath === TLNT_LANDING &&
      (!queryParams || Object.keys(queryParams).length === 0)
    ) {
      return false;
    }

    // =========================
    // BCDC rules
    // =========================
    const BCDC_ROOT = '/bao-cao-dia-chat';
    const BCDC_LANDING = '/bao-cao-dia-chat/vung-mo';

    // Root module url (/bao-cao-dia-chat) nếu menu của bạn trỏ về đây
    if (targetPath === BCDC_ROOT) {
      return currentPathOnly === BCDC_LANDING;
    }

    // Root "Báo cáo địa chất" (node không có queryParams)
    if (
      targetPath === BCDC_LANDING &&
      (!queryParams || Object.keys(queryParams).length === 0)
    ) {
      if (currentPathOnly !== BCDC_LANDING) return false;

      const currentQ = this.route.snapshot.queryParams || {};
      //sáng khi landing và KHÔNG chọn vùng (không có idVungMo)
      return !currentQ['idVungMo'];
    }

    if (targetPath === BCDC_LANDING && queryParams?.['idVungMo'] != null) {
      if (currentPathOnly !== BCDC_LANDING) return false;

      const currentQ = this.route.snapshot.queryParams || {};
      return (
        String(currentQ['idKhoangSan'] ?? '') ===
          String(queryParams['idKhoangSan'] ?? '') &&
        String(currentQ['idVungMo'] ?? '') ===
          String(queryParams['idVungMo'] ?? '')
      );
    }
    // =========================
    // DAPA rules
    // =========================
    const DAPA_ROOT = '/de-an-phuong-an';
    const DAPA_LANDING = '/de-an-phuong-an/vung-mo';
    const DAPA_TIEN_DO_THI_CONG =
      '/de-an-phuong-an/dapa-quan-ly-tien-do-thi-cong';
    const DAPA_CHI_TIET_TIEN_DO = '/de-an-phuong-an/chi-tiet-tien-do-thi-cong';

    // Root module url (/de-an-phuong-an)
    if (targetPath === DAPA_ROOT) {
      return currentPathOnly === DAPA_LANDING;
    }

    // Root "Đề án phương án" (node không có queryParams)
    if (
      targetPath === DAPA_LANDING &&
      (!queryParams || Object.keys(queryParams).length === 0)
    ) {
      if (currentPathOnly !== DAPA_LANDING) return false;
      return !currentQ['idVungMo']; // sáng khi landing và chưa chọn vùng
    }

    // Region node: phải check cả idKhoangSan và idVungMo
    if (targetPath === DAPA_LANDING && queryParams?.['idVungMo'] != null) {
      if (currentPathOnly !== DAPA_LANDING) return false;

      return (
        String(currentQ['idKhoangSan'] ?? '') ===
          String(queryParams['idKhoangSan'] ?? '') &&
        String(currentQ['idVungMo'] ?? '') ===
          String(queryParams['idVungMo'] ?? '')
      );
    }

    if (targetPath === DAPA_TIEN_DO_THI_CONG && queryParams) {
      if (currentPathOnly === DAPA_TIEN_DO_THI_CONG) {
        return (
          String(currentQ['idKhoangSan'] ?? '') ===
            String(queryParams['idKhoangSan'] ?? '') &&
          String(currentQ['idKhuMo'] ?? '') ===
            String(queryParams['idKhuMo'] ?? '') &&
          String(currentQ['dean'] ?? '') === String(queryParams['dean'] ?? '')
        );
      }
      if (currentPathOnly === DAPA_CHI_TIET_TIEN_DO) {
        return (
          String(currentQ['idKhoangSan'] ?? '') ===
            String(queryParams['idKhoangSan'] ?? '') &&
          String(currentQ['dean'] ?? '') === String(queryParams['dean'] ?? '')
        );
      }
      return false;
    }
    const QTTN_ROOT = '/quan-tri-tai-nguyen';

    if (targetPath === QTTN_ROOT) {
      return currentPathOnly.startsWith(QTTN_ROOT);
    }

    // =========================
    // DAKT (Dự án khai thác) rules
    // =========================
    const DAKT_ROOT = '/du-an-khai-thac';
    const DAKT_LANDING = '/du-an-khai-thac/vung-mo';

    // Root module url (/du-an-khai-thac)
    if (targetPath === DAKT_ROOT) {
      return currentPathOnly === DAKT_LANDING;
    }

    // Root "Đóng cửa mỏ" (node không có queryParams)
    if (
      targetPath === DAKT_LANDING &&
      (!queryParams || Object.keys(queryParams).length === 0)
    ) {
      if (currentPathOnly !== DAKT_LANDING) return false;
      return !currentQ['idVungMo']; // sáng khi landing và chưa chọn vùng
    }

    if (targetPath === DAKT_LANDING && queryParams?.['idVungMo'] != null) {
      if (currentPathOnly !== DAKT_LANDING) return false;

      return (
        String(currentQ['idVungMo'] ?? '') ===
        String(queryParams['idVungMo'] ?? '')
      );
    }
    // =========================
    // DCM (Đóng cửa mỏ) rules
    // =========================
    const DCM_ROOT = '/dong-cua-mo';
    const DCM_LANDING = '/dong-cua-mo/vung-mo';

    // Root module url (/dong-cua-mo)
    if (targetPath === DCM_ROOT) {
      return currentPathOnly === DCM_LANDING;
    }

    // Root "Đóng cửa mỏ" (node không có queryParams)
    if (
      targetPath === DCM_LANDING &&
      (!queryParams || Object.keys(queryParams).length === 0)
    ) {
      if (currentPathOnly !== DCM_LANDING) return false;
      return !currentQ['idVungMo']; // sáng khi landing và chưa chọn vùng
    }

    if (targetPath === DCM_LANDING && queryParams?.['idVungMo'] != null) {
      if (currentPathOnly !== DCM_LANDING) return false;

      return (
        String(currentQ['idVungMo'] ?? '') ===
        String(queryParams['idVungMo'] ?? '')
      );
    }

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
}
