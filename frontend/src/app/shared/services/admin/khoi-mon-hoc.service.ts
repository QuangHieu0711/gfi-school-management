import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  KHOI_MON_HOC_API_ENDPOINT,
  KhoiMonHocAssignRequest,
  KhoiMonHocDetailResponse,
} from '@app/model/admin/khoi-mon-hoc.model';

@Injectable({ providedIn: 'root' })
export class KhoiMonHocService {
  private readonly baseUrl = `${environment.host_api}/${KHOI_MON_HOC_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  assign(payload: KhoiMonHocAssignRequest) {
    return this.http.post<IResponse<null>>(
      `${this.baseUrl}/${KHOI_MON_HOC_API_ENDPOINT.ASSIGN}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  getDetail(gradeLevelId: number | string) {
    return this.http.get<IResponse<KhoiMonHocDetailResponse>>(
      `${this.baseUrl}/${gradeLevelId}`,
      {
        context: this.silentContext,
      }
    );
  }
}
