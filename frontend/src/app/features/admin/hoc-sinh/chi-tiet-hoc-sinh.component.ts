import { CommonModule, Location } from '@angular/common';
import { Component, Injector } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntil } from 'rxjs';

import { IconComponent } from '@components/app-icon/app-icon.component';
import { ComponentBaseAbstract } from '@layout';

import {
  HOC_SINH_DETAIL_FALLBACK,
  HocSinhDetailResponse,
} from '@app/model/admin/hoc-sinh.model';
import { HocSinhService } from '@app/service/admin/hoc-sinh.service';
import { HoSoHocSinhComponent } from './ho-so-hoc-sinh/ho-so-hoc-sinh.component';

@Component({
  selector: 'chi-tiet-hoc-sinh',
  standalone: true,
  templateUrl: './chi-tiet-hoc-sinh.component.html',
  styleUrls: ['./chi-tiet-hoc-sinh.component.scss'],
  imports: [CommonModule, HoSoHocSinhComponent, IconComponent],
})
export class ChiTietHocSinhComponent extends ComponentBaseAbstract {
  student: HocSinhDetailResponse | null = null;

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
