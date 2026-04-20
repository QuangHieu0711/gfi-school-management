import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  LOP_API_ENDPOINT,
  LopFilterRequest,
  LopFormRequest,
  LopResponse,
} from '@app/model/admin/lop.model';

@Injectable({ providedIn: 'root' })
export class LopService {
  private readonly baseUrl = `${environment.host_api}/${LOP_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: LopFilterRequest) {
    return this.http.post<IResponse<ITableResponse<LopResponse>>>(
      `${this.baseUrl}/${LOP_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<LopResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  getOptions(params?: {
    unitId?: number | string;
    gradeLevelId?: number | string;
    schoolYearId?: number | string;
  }) {
    const queryParams: Record<string, string | number> = {};
    if (params?.unitId) queryParams['unitId'] = params.unitId;
    if (params?.gradeLevelId) queryParams['gradeLevelId'] = params.gradeLevelId;
    if (params?.schoolYearId) queryParams['schoolYearId'] = params.schoolYearId;

    return this.http.get<IResponse<LopResponse[]>>(
      `${this.baseUrl}/${LOP_API_ENDPOINT.OPTIONS}`,
      { params: queryParams }
    );
  }

  create(payload: LopFormRequest) {
    return this.http.post<IResponse<LopResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: LopFormRequest) {
    return this.http.put<IResponse<LopResponse>>(
      `${this.baseUrl}/${id}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  delete(id: string | number) {
    return this.http.delete<IResponse<null>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }
}
