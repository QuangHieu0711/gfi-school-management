import { Injectable } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { SVG_ICONS } from '@constant/icons';

/**
 * A service that registers custom SVG icons for use with Angular Material `<mat-icon>`.
 * - Loads all icons listed in `SVG_ICONS`.
 * - Sanitizes URLs for safe usage in the DOM.
 */
@Injectable({ providedIn: 'root' })
export class IconRegistryService {
  private readonly basePath = 'assets/icons';

  constructor(
    private readonly iconRegistry: MatIconRegistry,
    private readonly sanitizer: DomSanitizer
  ) {
    this.registerIcons();
  }

  /**
   * Registers all SVG icons defined in the SVG_ICONS constant.
   * Each icon is registered with its name and sanitized URL.
   */
  private registerIcon(iconName: string): void {
    this.iconRegistry.addSvgIcon(iconName, this.sanitizer.bypassSecurityTrustResourceUrl(this.getIconUrl(iconName)));
  }

  /**
   * Registers all icons defined in the SVG_ICONS array.
   * This method is called in the constructor to ensure icons are available throughout the application.
   */
  private registerIcons(): void {
    for (const iconName of SVG_ICONS) this.registerIcon(iconName);
  }

  /**
   * Constructs the URL for the SVG icon based on its name.
   * @param iconName The name of the icon to construct the URL for.
   * @returns The full URL to the SVG icon.
   */
  private getIconUrl(iconName: string): string {
    return `${this.basePath}/${iconName}.svg`;
  }
}
