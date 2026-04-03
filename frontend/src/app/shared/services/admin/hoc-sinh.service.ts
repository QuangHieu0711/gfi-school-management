import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  HOC_SINH_API_ENDPOINT,
  HocSinhDetailResponse,
  HocSinhFilterRequest,
  HocSinhResponse,
} from '@app/model/admin/hoc-sinh.model';

@Injectable({ providedIn: 'root' })
export class HocSinhService {
  private readonly baseUrl = `${environment.host_api}/${HOC_SINH_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: HocSinhFilterRequest) {
    return this.http.post<IResponse<ITableResponse<HocSinhResponse>>>(
      `${this.baseUrl}/${HOC_SINH_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<HocSinhDetailResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }
}
