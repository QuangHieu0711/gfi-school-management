import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntil } from 'rxjs';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';

import {
  HOC_SINH_DETAIL_FALLBACK,
  HocSinhDetailResponse,
  HocSinhGuardian,
  HocSinhAddress,
} from '@app/model/admin/hoc-sinh.model';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';

@Component({
  selector: 'chi-tiet-hoc-sinh',
  standalone: true,
  templateUrl: './chi-tiet-hoc-sinh.component.html',
  styleUrls: ['./chi-tiet-hoc-sinh.component.scss'],
  imports: [CommonModule, IconComponent],
})
export class ChiTietHocSinhComponent extends ComponentBaseAbstract {
  student: HocSinhDetailResponse | null = null;

  // ── Section collapse state ─────────────────────────────
  collapsedSections = new Set<string>();

  toggleSection(key: string): void {
    if (this.collapsedSections.has(key)) {
      this.collapsedSections.delete(key);
    } else {
      this.collapsedSections.add(key);
    }
  }

  isSectionCollapsed(key: string): boolean {
    return this.collapsedSections.has(key);
  }

  constructor(
    protected override injector: Injector,
    private readonly routeService: ActivatedRoute,
    private readonly hocSinhService: HocSinhService,
    private readonly locationService: Location
  ) {
    super(injector);
  }

  protected override componentInit(): void {
    const routeState = history.state?.student as
      | HocSinhDetailResponse
      | undefined;
    this.student = routeState ?? HOC_SINH_DETAIL_FALLBACK;

    this.routeService.paramMap
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((params) => {
        const id = params.get('id');
        if (!id) return;

        this.hocSinhService.getById(id).subscribe({
          next: ({ data }) => {
            this.student = this.mergeStudent(data);
          },
          error: () => {
            this.student = this.mergeStudent(
              routeState ?? HOC_SINH_DETAIL_FALLBACK
            );
          },
        });
      });
  }

  goBack(): void {
    this.locationService.back();
  }

  // ── Safe field helpers (bypass index-signature strict check) ──
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getField(key: string): string {
    if (!this.student) return '—';
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const val = (this.student as any)[key];
    return this.fmt(val);
  }

  getEnrollment(key: string): string {
    if (!this.student?.enrollment) return '—';
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const val = (this.student.enrollment as any)[key];
    return this.fmt(val);
  }

  guardianField(g: HocSinhGuardian | null, key: keyof HocSinhGuardian): string {
    if (!g) return '—';
    return this.fmt(g[key]);
  }

  addrField(a: HocSinhAddress | null, key: keyof HocSinhAddress): string {
    if (!a) return '—';
    return this.fmt(a[key]);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private fmt(val: any): string {
    if (val === null || val === undefined || val === '') return '—';
    if (typeof val === 'boolean') return val ? 'Có' : 'Không';
    return String(val);
  }

  formatGender(): string {
    if (!this.student) return '—';
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const val = (this.student as any)['gender'];
    if (!val && val !== 0) return '—';
    const s = String(val).toLowerCase();
    if (s === '0' || s === 'nam') return 'Nam';
    if (s === '1' || s === 'nu' || s === 'nữ') return 'Nữ';
    return String(val);
  }

  formatBool(val: boolean | null | undefined): string {
    return val ? 'Có' : 'Không';
  }

  // ── Guardian helpers ───────────────────────────────────
  get father(): HocSinhGuardian | null {
    return (
      this.student?.guardians?.find((g) =>
        ['CHA', 'FATHER'].includes((g.guardianType ?? '').toUpperCase())
      ) ?? null
    );
  }

  get mother(): HocSinhGuardian | null {
    return (
      this.student?.guardians?.find((g) =>
        ['ME', 'MOTHER'].includes((g.guardianType ?? '').toUpperCase())
      ) ?? null
    );
  }

  // ── Address helpers ────────────────────────────────────
  get permanentAddress(): HocSinhAddress | null {
    return (
      this.student?.addresses?.find((a) =>
        (a.addressType ?? '').toLowerCase().includes('thuong')
      ) ?? null
    );
  }

  get temporaryAddress(): HocSinhAddress | null {
    return (
      this.student?.addresses?.find((a) =>
        (a.addressType ?? '').toLowerCase().includes('tam')
      ) ?? null
    );
  }

  private mergeStudent(data: HocSinhDetailResponse): HocSinhDetailResponse {
    return {
      ...HOC_SINH_DETAIL_FALLBACK,
      ...data,
      enrollment: {
        ...HOC_SINH_DETAIL_FALLBACK.enrollment,
        ...data.enrollment,
      },
      profile: {
        ...HOC_SINH_DETAIL_FALLBACK.profile,
        ...data.profile,
      },
      guardians: data.guardians?.length
        ? data.guardians
        : HOC_SINH_DETAIL_FALLBACK.guardians,
      addresses: data.addresses?.length
        ? data.addresses
        : HOC_SINH_DETAIL_FALLBACK.addresses,
    };
  }
}
