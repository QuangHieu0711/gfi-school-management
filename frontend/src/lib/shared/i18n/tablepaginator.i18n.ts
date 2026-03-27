import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { LanguageService } from '@service';

@Injectable()
export class TablePaginatorIntl extends MatPaginatorIntl {
  constructor(private readonly languageService: LanguageService) {
    super();
    this.loadTranslations();
  }

  private loadTranslations(): void {
    this.itemsPerPageLabel = this.languageService.instant('table.paginator.items_per_page_label');
    this.nextPageLabel = this.languageService.instant('table.paginator.next_page_label');
    this.previousPageLabel = this.languageService.instant('table.paginator.previous_page_label');
    this.firstPageLabel = this.languageService.instant('table.paginator.first_page_label');
    this.lastPageLabel = this.languageService.instant('table.paginator.last_page_label');
    this.getRangeLabel = (page: number, pageSize: number, length: number) => {
      if (length === 0 || pageSize === 0) {
        return this.languageService.instant('table.paginator.range_label_length_0');
      }

      const startIndex = page * pageSize;
      return this.languageService.instant('table.paginator.range_label', {
        start: startIndex + 1,
        end: Math.min(startIndex + pageSize, length),
        length,
      });
    };

    this.changes.next(); // Notify updates
  }
}
