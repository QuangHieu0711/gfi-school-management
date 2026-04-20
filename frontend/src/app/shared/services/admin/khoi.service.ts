import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import { Observable } from 'rxjs';

import {
  KHOI_API_ENDPOINT,
  KhoiFilterRequest,
  KhoiFormRequest,
  KhoiOptionResponse,
  KhoiResponse,
} from '@app/model/admin/khoi.model';

@Injectable({ providedIn: 'root' })
export class KhoiService {
  private readonly baseUrl = `${environment.host_api}/${KHOI_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: KhoiFilterRequest
  ): Observable<IResponse<ITableResponse<KhoiResponse> | KhoiResponse[]>> {
    return this.http.post<
      IResponse<ITableResponse<KhoiResponse> | KhoiResponse[]>
    >(`${this.baseUrl}/${KHOI_API_ENDPOINT.FILTER}`, payload);
  }

  getById(id: string | number) {
    return this.http.get<IResponse<KhoiResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  getOptions() {
    return this.http.get<IResponse<KhoiOptionResponse[]>>(
      `${this.baseUrl}/${KHOI_API_ENDPOINT.OPTIONS}`
    );
  }

  create(payload: KhoiFormRequest) {
    return this.http.post<IResponse<KhoiResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: KhoiFormRequest) {
    return this.http.put<IResponse<KhoiResponse>>(
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
