import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  DASHBOARD_API_ENDPOINT,
  DashboardSummary,
} from '@app/model/admin/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = `${environment.host_api}/${DASHBOARD_API_ENDPOINT.BASE_PATH}/stats`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getSummary(unitId?: number) {
    return this.http.get<IResponse<DashboardSummary>>(this.baseUrl, {
      context: this.silentContext,
      params:
        unitId != null
          ? {
              unitId: String(unitId),
            }
          : undefined,
    });
  }
}
