import { CommonModule } from '@angular/common';
import { Component, Injector } from '@angular/core';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';

import {
  DashboardAttendanceItem,
  DashboardDistributionItem,
  DashboardSummary,
} from '@app/model/admin/dashboard.model';
import { DashboardService } from '@app/service/admin/dashboard.service';

@Component({
  selector: 'admin-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [CommonModule, IconComponent],
})
export class DashboardComponent extends ComponentBaseAbstract {
  loading = false;
  summary?: DashboardSummary;

  private readonly numberFormat = new Intl.NumberFormat('vi-VN');

  constructor(
    protected override injector: Injector,
    private readonly dashboardService: DashboardService
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    this.loadSummary();
  }

  formatNumber(value?: number | null): string {
    const safeValue = Number(value ?? 0);
    return this.numberFormat.format(safeValue);
  }

  formatPercent(value?: number | null): string {
    const raw = Number(value ?? 0);
    const normalized = raw > 1 ? raw / 100 : raw;
    return `${Math.round(normalized * 100)}%`;
  }

  percentValue(value?: number | null): number {
    const raw = Number(value ?? 0);
    const normalized = raw > 1 ? raw / 100 : raw;
    return Math.min(Math.max(normalized * 100, 0), 100);
  }

  maxDistribution(list: DashboardDistributionItem[] = []): number {
    return Math.max(1, ...list.map((item) => Number(item.value ?? 0)));
  }

  maxAttendanceTotal(list: DashboardAttendanceItem[] = []): number {
    return Math.max(
      1,
      ...list.map(
        (item) => Number(item.presentCount ?? 0) + Number(item.absentCount ?? 0)
      )
    );
  }

  percentOf(value: number, max: number): number {
    if (!max) return 0;
    return Math.min(Math.max((value / max) * 100, 0), 100);
  }

  trackByLabel(_: number, item: DashboardDistributionItem): string {
    return item.label;
  }

  private loadSummary(): void {
    this.loading = true;
    this.dashboardService.getSummary().subscribe({
      next: ({ data }) => {
        this.summary = data;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.toastr.error(
          error?.error?.userMessage ??
            error?.error?.message ??
            'Không tải được dữ liệu dashboard',
          'Thất bại'
        );
      },
    });
  }
}
