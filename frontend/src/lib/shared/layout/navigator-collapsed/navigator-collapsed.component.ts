/* eslint-disable @typescript-eslint/no-explicit-any */
import { Component, Input, Injector } from '@angular/core';
import { ISidebarItem } from '@model/menu.model';
import { ComponentBaseAbstract } from '@layout';
import { MATERIAL_MODULE } from '@modules';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { IconComponent } from '@components/app-icon/app-icon.component';
import { NAVIGATOR_ENDPOINT } from '@constant/navigator';
// router is provided by base ComponentBaseAbstract via the injector

@Component({
  selector: 'app-navigator-collapsed-component',
  templateUrl: './navigator-collapsed.component.html',
  styleUrls: ['./navigator-collapsed.component.scss'],
  imports: [CommonModule, ...MATERIAL_MODULE, RouterModule, IconComponent],
})
export class NavigatorCollapsedComponent extends ComponentBaseAbstract {
  @Input() menu: ISidebarItem[] = [];
  constructor(protected override injector: Injector) {
    super(injector);
  }

  navigateItem(item: ISidebarItem): void {
    const url = (item as any).url ?? item.url;
    const queryParams = (item as any).queryParams;
    if (queryParams && url) {
      const commands = String(url)
        .split('/')
        .filter((p) => p !== '');
      this.router.navigate(['/', ...commands], { queryParams });
      return;
    }

    if (url) {
      this.router.navigateByUrl(String(url));
      return;
    }
  }
  override isPathActive(item: ISidebarItem): boolean {
    const currentPathOnly = this.router.url.split('?')[0] || '/';

    // Map key -> module root (đảm bảo icon luôn sáng khi đang ở bất kỳ trang con)
    const rootByKey: Record<string, string> = {
      dashboard: '/',
      taiLieuNguyenThuy: `/${NAVIGATOR_ENDPOINT.TAI_LIEU_NGUYEN_THUY.TAI_LIEU_NGUYEN_THUY}`,
      baoCaoDiaChat: `/${NAVIGATOR_ENDPOINT.BAO_CAO_DIA_CHAT.BAO_CAO_DIA_CHAT}`,
      deAnPhuongAn: `/${NAVIGATOR_ENDPOINT.DE_AN_PHUONG_AN.BASE_PATH}`,
      dongCuaMo: `/${NAVIGATOR_ENDPOINT.DONG_CUA_MO.BASE_PATH}`,
      quanTriTaiNguyen: `/${NAVIGATOR_ENDPOINT.QUAN_TRI_TAI_NGUYEN.BASE_PATH}`,
    };

    // Ưu tiên root theo key (đặc biệt fix cho QTTN)
    let targetPath =
      rootByKey[(item as any)?.key] ??
      String((item as any)?.url ?? item.url ?? '');

    if (!targetPath) return false;
    if (!targetPath.startsWith('/')) targetPath = `/${targetPath}`;

    // Active khi đang ở root hoặc bất kỳ route con nào
    return (
      currentPathOnly === targetPath ||
      currentPathOnly.startsWith(`${targetPath}/`)
    );
  }

  // override isPathActive(item: ISidebarItem): boolean {
  //   const rawUrl = (item as any)?.url ?? item.url;
  //   if (!rawUrl) return false;

  //   const targetPath = String(rawUrl).startsWith('/')
  //     ? String(rawUrl)
  //     : `/${rawUrl}`;

  //   const currentPathOnly = this.router.url.split('?')[0] || '/';

  //   // Trang chủ (tuỳ dự án bạn có thể dùng '/' hoặc '/dashboard')
  //   if (targetPath === '/' || targetPath === '') {
  //     return currentPathOnly === '/';
  //   }

  //   // ✅ Module icon: sáng khi đang ở bất kỳ route con nào
  //   return (
  //     currentPathOnly === targetPath ||
  //     currentPathOnly.startsWith(`${targetPath}/`)
  //   );
  // }
}
