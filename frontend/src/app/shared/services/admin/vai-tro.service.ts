import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import { Observable } from 'rxjs';

import {
  VAI_TRO_API_ENDPOINT,
  VaiTroFilterRequest,
  VaiTroFormRequest,
  VaiTroOptionResponse,
  VaiTroResponse,
} from '@app/model/admin/vai-tro.model';

@Injectable({ providedIn: 'root' })
export class VaiTroService {
  private readonly baseUrl = `${environment.host_api}/${VAI_TRO_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: VaiTroFilterRequest
  ): Observable<IResponse<ITableResponse<VaiTroResponse>>> {
    return this.http.post<IResponse<ITableResponse<VaiTroResponse>>>(
      `${this.baseUrl}/${VAI_TRO_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<VaiTroResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  getOptions() {
    return this.http.get<IResponse<VaiTroOptionResponse[]>>(
      `${this.baseUrl}/${VAI_TRO_API_ENDPOINT.OPTIONS}`
    );
  }

  create(payload: VaiTroFormRequest) {
    return this.http.post<IResponse<VaiTroResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: VaiTroFormRequest) {
    return this.http.put<IResponse<VaiTroResponse>>(
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
