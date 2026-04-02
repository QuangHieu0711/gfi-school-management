import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  MON_HOC_API_ENDPOINT,
  MonHocFilterRequest,
  MonHocFormRequest,
  MonHocResponse,
} from '@app/model/admin/mon-hoc.model';

@Injectable({ providedIn: 'root' })
export class MonHocService {
  private readonly baseUrl = `${environment.host_api}/${MON_HOC_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: MonHocFilterRequest) {
    return this.http.post<IResponse<ITableResponse<MonHocResponse>>>(
      `${this.baseUrl}/${MON_HOC_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<MonHocResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  create(payload: MonHocFormRequest) {
    return this.http.post<IResponse<MonHocResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: MonHocFormRequest) {
    return this.http.put<IResponse<MonHocResponse>>(`${this.baseUrl}/${id}`, payload, {
      context: this.silentContext,
    });
  }

  delete(id: string | number) {
    return this.http.delete<IResponse<null>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }
}
