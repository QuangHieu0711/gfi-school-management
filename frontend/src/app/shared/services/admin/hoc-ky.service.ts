import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import { Observable } from 'rxjs';

import {
  HOC_KY_API_ENDPOINT,
  HocKyFilterRequest,
  HocKyFormRequest,
  HocKyResponse,
} from '@app/model/admin/hoc-ky.model';

@Injectable({ providedIn: 'root' })
export class HocKyService {
  private readonly baseUrl = `${environment.host_api}/${HOC_KY_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: HocKyFilterRequest
  ): Observable<IResponse<ITableResponse<HocKyResponse>>> {
    return this.http.post<IResponse<ITableResponse<HocKyResponse>>>(
      `${this.baseUrl}/${HOC_KY_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<HocKyResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  create(payload: HocKyFormRequest) {
    return this.http.post<IResponse<HocKyResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: HocKyFormRequest) {
    return this.http.put<IResponse<HocKyResponse>>(
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
