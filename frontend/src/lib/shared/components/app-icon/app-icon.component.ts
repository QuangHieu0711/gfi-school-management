import { CommonModule } from '@angular/common';
import { Component, HostBinding, Input } from '@angular/core';
import { ICON_PREFIXES, ICONS_TYPE, IconType } from '@model/icon.model';
import { MATERIAL_MODULE } from '@modules';

@Component({
  selector: 'app-icon',
  templateUrl: './app-icon.component.html',
  styleUrls: ['./app-icon.component.scss'],
  imports: [CommonModule, ...MATERIAL_MODULE],
  standalone: true,
})
export class IconComponent {
  @HostBinding('style.width.px') get hostWidth() { return this.size; }
  @HostBinding('style.height.px') get hostHeight() { return this.size; }

  /**
   * Main icon identifier.
   * One string input that determines which kind of icon to render
   * Supports prefixed types:
   * - `svg:icon-name` → Material SVG icon (registered via SvgIconRegistryService)
   * - `lucide:icon-name` → Lucide icon component
   * - `mat:icon-name` or `icon-name` → Angular Material font icon
   */
  @Input() icon = '';

  /**
   * Icon size in pixels.
   * Used to control both width and height.
   */
  @Input() size = 24;

  /**
   * Tailwind color class suffix (e.g., 'error-500', 'neutral-700').
   * Will be prefixed with `text-`. Defaults to `'primary-500'`.
   * Ignored if `color` is provided.
   */
  @Input() variant = '';

  /**
   * Raw CSS color (hex, rgb, hsl, etc.).
   * Example: `#ff0000`, `rgb(0,0,0)`
   * Takes precedence over `variant` if defined.
   */
  @Input() color = '';

  /**
   * Additional CSS classes to apply to the icon.
   */
  @Input() iconClass = '';

  /**
   * Extracted icon name without prefix.
   */
  get iconName(): string {
    return this.icon.includes(':') ? this.icon.split(':')[1] : this.icon;
  }

  /**
   * Determine the rendering strategy based on icon prefix.
   * - `svg:` → Material SVG registry
   * - Default → Material font icon
   */
  get iconType(): IconType {
    if (this.icon.startsWith(ICON_PREFIXES.SVG)) return ICONS_TYPE.SVG;
    if (this.icon.startsWith(ICON_PREFIXES.MAT)) return ICONS_TYPE.MAT;
    return ICONS_TYPE.MAT; // fallback default
  }

  /**
   * Tailwind-based class string, includes:
   * - width & height
   * - `text-{variant}` if no raw color is set
   * - any custom class
   */
  get computedClass(): string {
    const sizeClass = `!w-[${this.size}px] !h-[${this.size}px]`;
    const colorClass = this.color ? `!text-[${this.color}]` : `!text-${this.variant}`;
    return [sizeClass, colorClass, this.iconClass].filter(Boolean).join(' ');
  }

  /**
   * Inline styles for the icon element.
   * Only includes `color` if hex/RGB is set.
   */
  get computedStyle(): Record<string, string> {
    return {
      width: `${this.size}px`,
      height: `${this.size}px`,
      'font-size': `${this.size}px`,
      'line-height': `${this.size}px`,
      ...(this.color ? { color: this.color } : {}),
    };
  }

  readonly ICONS_TYPE = ICONS_TYPE;
}
