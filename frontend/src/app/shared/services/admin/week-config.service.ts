import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  WEEK_CONFIG_API_ENDPOINT,
  WeekConfigBulkUpdateRequest,
  WeekConfigGenerateRequest,
  WeekConfigOptionResponse,
  WeekConfigQueryRequest,
  WeekConfigResponse,
  WeekConfigUpdateRequest,
} from '@app/model/admin/week-config.model';

@Injectable({ providedIn: 'root' })
export class WeekConfigService {
  private readonly baseUrl = `${environment.host_api}/${WEEK_CONFIG_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getList(payload: WeekConfigQueryRequest) {
    let params = new HttpParams().set(
      'schoolYearId',
      String(payload.schoolYearId)
    );

    if (payload.semesterId !== undefined && payload.semesterId !== null) {
      params = params.set('semesterId', String(payload.semesterId));
    }

    return this.http.get<
      IResponse<WeekConfigResponse[] | ITableResponse<WeekConfigResponse>>
    >(this.baseUrl, { params });
  }

  generate(payload: WeekConfigGenerateRequest) {
    return this.http.post<IResponse<WeekConfigResponse[]>>(
      `${this.baseUrl}/${WEEK_CONFIG_API_ENDPOINT.GENERATE}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  bulkUpdate(payload: WeekConfigBulkUpdateRequest) {
    return this.http.post<IResponse<WeekConfigResponse[]>>(
      `${this.baseUrl}/${WEEK_CONFIG_API_ENDPOINT.BULK_UPDATE}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  update(id: string | number, payload: WeekConfigUpdateRequest) {
    return this.http.put<IResponse<WeekConfigResponse | WeekConfigResponse[]>>(
      `${this.baseUrl}/${id}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  deleteBySemester(semesterId: string | number) {
    return this.http.delete<IResponse<null>>(
      `${this.baseUrl}/${WEEK_CONFIG_API_ENDPOINT.BY_SEMESTER}/${semesterId}`,
      {
        context: this.silentContext,
      }
    );
  }

  getComboboxOptions() {
    return this.http.get<IResponse<WeekConfigOptionResponse[]>>(
      `${this.baseUrl}/${WEEK_CONFIG_API_ENDPOINT.CBB}`
    );
  }
}
