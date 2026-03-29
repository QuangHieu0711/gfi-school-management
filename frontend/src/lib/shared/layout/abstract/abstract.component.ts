import {
  ChangeDetectorRef,
  Directive,
  Injector,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { distinctUntilChanged, filter, Subject, takeUntil, tap } from 'rxjs';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { Location } from '@angular/common';
import { NAVIGATOR_ENDPOINT, PATH } from '@constant/navigator';
import {
  ApiService,
  DialogService,
  FileService,
  IconRegistryService,
  ItemControlService,
  LanguageService,
  LoadingService,
  StorageService,
  ToastService,
} from '@service';
import { State } from '@store/reducers';
import { FormType } from '@model/form-control.model';
import { EndpointTree, ISidebarItem } from '@model/menu.model';
import {
  FALLBACK_LANGUAGE,
  PAGINATION_DEFAULT_PAGE_SIZE,
  TYPE_FORM,
  TYPE_FORM_KEY,
} from '@constant/constant';
import { navigateTo, resolvePath } from '@utils/resolved-path';
import { AppTableComponent } from '@components/app-table/app-table.component';
import { ICurrentUser, IRule } from '@model/auth.model';
import { UserInfoAction } from '@store/user-info';
import { AppTableMergeComponent } from '@components/app-table-merge/app-table-merge.component';

@Directive()
export abstract class ComponentBaseAbstract implements OnInit, OnDestroy {
  @ViewChild(AppTableComponent) appTableComponent!: AppTableComponent;
  @ViewChild(AppTableMergeComponent) appTableMergeComponent!: AppTableComponent;
  public form: FormGroup = new FormGroup({});
  public ngUnsubscribe = new Subject();
  public dataSourceTotal = 0;
  public pageIndex = 0;
  public pageSize = PAGINATION_DEFAULT_PAGE_SIZE;
  public navigatorEndpoint = NAVIGATOR_ENDPOINT;
  public TYPE_FORM = TYPE_FORM;
  public pathType: TYPE_FORM_KEY = this.TYPE_FORM.CREATE;
  public userRules: IRule[] = [];
  public get rules() {
    return this.userRules.find((r) => this.router.url.includes(r.url));
  }

  // Declare injector
  protected router: Router;
  protected route: ActivatedRoute;
  protected location: Location;
  protected cdRef: ChangeDetectorRef;
  protected store: Store<State>;
  protected toastr: ToastService;
  protected httpClient: ApiService;
  protected loadingIndicator: LoadingService;
  protected itemControl: ItemControlService;
  protected dialog: DialogService;
  protected translateService: TranslateService;
  protected i18n: LanguageService;
  protected storage: StorageService;
  protected fileService: FileService;

  /**
   * Base component constructor.
   * This constructor initializes the component with the provided injector.
   * It retrieves various services from the injector, such as Router, ActivatedRoute, Location, ChangeDetectorRef, Store, ToastService, ApiService, LoadingService, ItemControlService, DialogService, TranslateService, and LanguageService.
   * @param injector The Angular injector used to retrieve services.
   */
  public constructor(protected injector: Injector) {
    this.router = injector.get(Router);
    this.route = injector.get(ActivatedRoute);
    this.location = injector.get(Location);
    this.cdRef = injector.get(ChangeDetectorRef);
    this.store = injector.get(Store);
    this.toastr = injector.get(ToastService);
    this.httpClient = injector.get(ApiService);
    this.loadingIndicator = injector.get(LoadingService);
    this.itemControl = injector.get(ItemControlService);
    this.dialog = injector.get(DialogService);
    this.translateService = injector.get(TranslateService);
    this.i18n = injector.get(LanguageService);
    this.storage = injector.get(StorageService);
    this.fileService = injector.get(FileService);
    injector.get(IconRegistryService);
  }

  ngOnInit() {
    this.translateService.setDefaultLang(FALLBACK_LANGUAGE);
    this.i18n.setDefaultLanguage();

    this.initUserRules();
    this.componentInit();
  }

  /**
   * Initialize the component.
   * This method is called after the component is created and before it is rendered.
   * It can be used to set up initial state, fetch data, or perform any setup tasks.
   */
  protected componentInit(): void {
    // Component init
  }

  /**
   * Navigate to the login page when access is denied.
   * This method can be used to redirect users to the login page if they do not have the necessary permissions.
   */
  protected gotoAccessDenied() {
    navigateTo([`LOGIN`]);
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next(true);
    this.ngUnsubscribe.unsubscribe();
    this.componentDestroy();
  }

  /**
   * Cleanup logic when the component is destroyed.
   * This method is called when the component is about to be destroyed.
   * It can be used to release resources, unsubscribe from observables, or perform any necessary cleanup tasks.
   */
  protected componentDestroy(): void {
    // Component destroy
  }

  /**
   * Finds a form control by its key in the provided form item array.
   * @param formItem The array of form controls to search.
   * @param key The key of the form control to find.
   * @returns The found form control.
   * @throws Error if the form control with the specified key is not found.
   */
  public findFormControl(formItem: FormType[], key: string): FormType {
    const control: FormType | undefined = formItem.find(
      (item) => item.key === key
    );
    if (!control) {
      throw new Error(`Form control with key '${key}' not found`);
    }
    return control;
  }

  /**
   * Check if the given path is active in the router.
   * @param path The path to check.
   * @returns True if the path is active, false otherwise.
   */
  public isPathActive(
    pathOrNode?:
      | string
      | { url?: string; queryParams?: Record<string, unknown> }
  ): boolean {
    if (!pathOrNode) return false;

    let path = '';
    let nodeQuery: Record<string, unknown> | undefined;

    if (typeof pathOrNode === 'string') path = pathOrNode;
    else {
      path = pathOrNode.url ?? '';
      nodeQuery = pathOrNode.queryParams as Record<string, unknown> | undefined;
    }
    if (!path) return false;

    if (path === '/') return path === this.router.url;

    path = path.startsWith('/') ? path : `/${path}`;
    const currentPathOnly = this.router.url.split('?')[0];
    const currentQ = this.route.snapshot.queryParams || {};

    // ================= TLNT rules (giữ như bạn đang có) =================
    const TLNT_ROOT = '/tai-lieu-nguyen-thuy';
    const TLNT_LANDING = '/tai-lieu-nguyen-thuy/vung-mo';

    if (path === TLNT_ROOT) {
      return currentPathOnly === TLNT_LANDING && !currentQ['idVungMo'];
    }

    if (
      path === TLNT_LANDING &&
      (!nodeQuery || Object.keys(nodeQuery).length === 0)
    ) {
      return false;
    }

    // ================= ✅ BCDC rules (PHẢI đặt TRƯỚC default) =================
    const BCDC_LANDING = '/bao-cao-dia-chat/vung-mo';

    if (path === BCDC_LANDING) {
      // ❗ chỉ tính active khi đang đúng landing, không sáng ở /vung-mo/khu-mo...
      if (currentPathOnly !== BCDC_LANDING) return false;

      // Root "Báo cáo địa chất" (không query): sáng khi không có idVungMo
      if (!nodeQuery || Object.keys(nodeQuery).length === 0) {
        return !currentQ['idVungMo'];
      }

      // Node "Vùng ..." (có idVungMo): sáng khi đúng idVungMo
      if (nodeQuery['idVungMo'] != null) {
        return (
          String(currentQ['idVungMo'] ?? '') ===
          String(nodeQuery['idVungMo'] ?? '')
        );
      }
    }

    // ================= default behavior =================
    const pathOk = this.router.isActive(path, {
      paths: 'subset',
      queryParams: 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored',
    });

    if (!pathOk) return false;

    if (nodeQuery && Object.keys(nodeQuery).length > 0) {
      const keys = Object.keys(nodeQuery).sort();
      for (const k of keys) {
        const expected = String(nodeQuery[k] ?? '');
        const actual = String(currentQ[k] ?? '');
        if (expected !== actual) return false;
      }
    }

    return true;
  }

  /**
   * Check if any child path is active.
   * @param children The array of sidebar items to check.
   * @returns True if any child path is active, false otherwise.
   */
  public isChildPathActive(children: ISidebarItem[] = []): boolean {
    return children.some(
      (child) =>
        this.isPathActive(child) || this.isChildPathActive(child.children ?? [])
    );
  }

  /**
   * This method should be overridden in the derived class to implement specific reset logic.
   * For example, it could reset pagination, filters, or other query parameters.
   * The AppTableComponent will call this method to reset its query state.
   */
  public resetQuery(): void {
    this.pageIndex = 0;
  }

  /**
   * Force change detection to update the view.
   * This method uses setTimeout to ensure that change detection runs after the data is set.
   */
  public forceChangeDetection(): void {
    setTimeout(() => this.cdRef.detectChanges()); // Ensure change detection runs after the data is set
  }

  /**
   * Get the type of the current path based on the router URL.
   * This method checks the current URL to determine if it is for creating, updating, or viewing details.
   */
  public getTypeByPath() {
    const url = this.router.url;
    if (url.includes(PATH.TAO_MOI)) this.pathType = this.TYPE_FORM.CREATE;
    else if (url.includes(PATH.CAP_NHAT)) this.pathType = this.TYPE_FORM.UPDATE;
    else if (url.includes(PATH.CHI_TIET)) this.pathType = this.TYPE_FORM.DETAIL;
  }

  /**
   * Recursively resolves all sidebar menu items by transforming their `path` arrays
   * (e.g., ['DESIGN_SYSTEM', 'THEME', 'COLOR']) into actual URL strings using `resolvePath`.
   *
   * This method ensures that menu items are fully resolved before being rendered,
   * and supports nested menu trees by recursively resolving child items.
   *
   * @param menu The sidebar menu definition array, typically from a static `MENU` config.
   * @param endpoints The root `EndpointTree` object used to resolve menu item paths.
   * @returns A fully resolved array of `ISidebarItem` with concrete URLs.
   */
  public resolveMenu(
    menu: ISidebarItem[],
    endpoints: EndpointTree
  ): ISidebarItem[] {
    return menu.map((item) => ({
      ...item,
      url:
        item.path && Array.isArray(item.path)
          ? resolvePath(item.path)
          : item.url,
      children: item.children
        ? this.resolveMenu(item.children, endpoints)
        : undefined,
    }));
  }

  /**
   * Get the select all option for form controls.
   * This method returns an object representing the "select all" option for form controls,
   */
  public get selectAllOption() {
    return { label: 'Tất cả', value: '' };
  }

  private initUserRules(): void {
    this.store
      .select((state) => state.userInfo)
      .pipe(
        tap((u) => {
          if (!u?.id && this.storage.has('userInfo', 'all')) {
            this.store.dispatch(UserInfoAction.GetCurrentUser());
          }
        }),
        filter((u): u is ICurrentUser => !!u?.id),
        distinctUntilChanged((a, b) => a.id === b.id),
        tap((u) => {
          this.userRules = u.role.rules ?? [];
        }),
        takeUntil(this.ngUnsubscribe)
      )
      .subscribe();
  }
}
