import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MATERIAL_MODULE } from '@modules';
import { MtxSelectModule } from '@ng-matero/extensions/select';
import { PAGINATION_DELTA, PAGINATION_DEFAULT_PAGE_SIZE } from '@constant/constant';
import { TranslateModule } from '@ngx-translate/core';
import { IconComponent } from '@components/app-icon/app-icon.component';

@Component({
  selector: 'app-paginator',
  templateUrl: './app-paginator.component.html',
  imports: [...MATERIAL_MODULE, MtxSelectModule, FormsModule, TranslateModule, IconComponent],
})
export class AppPaginatorComponent {
  @Input() total = 0;
  @Input() pageIndex = 0;
  @Input() pageSize = 0;
  @Input() pageSizeOptions: number[] = [10, 25, 50, 100];
  @Output() pageChange = new EventEmitter<PageEvent>();

  get totalPages(): number {
    return Math.ceil(this.total / this.pageSize);
  }

  /**
   * Returns an array of page numbers to display in the paginator.
   * It includes the first page, last page, and ellipses for skipped pages.
   */
  get visiblePages(): number[] {
    const total = this.totalPages;
    const current = this.pageIndex;
    const delta = PAGINATION_DELTA;
    const pages: number[] = [];

    if (total <= PAGINATION_DEFAULT_PAGE_SIZE) return Array.from({ length: this.totalPages }, (_, i) => i);

    pages.push(0); // Always show first page

    const start = Math.max(current - delta, 1);
    const end = Math.min(current + delta, total - 2);

    if (start > 1) pages.push(-1); // 0 for ellipsis

    // Add middle pages
    for (let i = start; i <= end; i++) pages.push(i);

    // Add right dots if needed
    if (end < total - 2) pages.push(-1);

    pages.push(total - 1); // Always show last page

    return pages;
  }

  get startIndex(): number {
    return this.pageIndex * this.pageSize + 1;
  }

  get endIndex(): number {
    return Math.min(this.pageIndex * this.pageSize + this.pageSize, this.total);
  }

  goToPage(index: number) {
    if (index >= 0 && index < this.totalPages && index !== this.pageIndex)
      this.pageChange.emit({ pageIndex: index, pageSize: this.pageSize, length: this.total });
  }

  /**
   * Handles page change events from the paginator.
   */
  changePageSize(size: string) {
    this.pageChange.emit({ pageIndex: 0, pageSize: Number(size), length: this.total });
  }

  trackByFn(item: unknown): unknown {
    return (item as { value: unknown }).value;
  }

  /**
   * Handles click events on the ellipsis (dots) in the paginator.
   * It calculates the direction (left or right) and the jump size,
   * then navigates to the appropriate page.
   */
  onDotsClick(idx: number) {
    const currentPageIndex = this.visiblePages.indexOf(this.pageIndex);
    const position = idx < currentPageIndex ? 'left' : 'right';
    const jump = PAGINATION_DELTA * 2;

    if (position === 'left') this.goToPage(Math.max(0, this.pageIndex - jump));
    else this.goToPage(Math.min(this.totalPages - 1, this.pageIndex + jump));
  }
}
