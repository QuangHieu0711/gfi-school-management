import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import { Observable } from 'rxjs';

import {
  DON_VI_API_ENDPOINT,
  DonViFilterRequest,
  DonViFormRequest,
  DonViOptionResponse,
  DonViResponse,
} from '@app/model/admin/don-vi.model';

@Injectable({ providedIn: 'root' })
export class DonViService {
  private readonly baseUrl = `${environment.host_api}/${DON_VI_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: DonViFilterRequest
  ): Observable<IResponse<ITableResponse<DonViResponse>>> {
    return this.http.post<IResponse<ITableResponse<DonViResponse>>>(
      `${this.baseUrl}/${DON_VI_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<DonViResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  getOptions() {
    return this.http.get<IResponse<DonViOptionResponse[]>>(
      `${this.baseUrl}/${DON_VI_API_ENDPOINT.OPTIONS}`
    );
  }

  getCreateUserUnitOptions() {
    return this.http.get<IResponse<DonViOptionResponse[]>>(
      `${this.baseUrl}/${DON_VI_API_ENDPOINT.USER_CREATION_OPTIONS}`
    );
  }

  create(payload: DonViFormRequest) {
    return this.http.post<IResponse<DonViResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: DonViFormRequest) {
    return this.http.put<IResponse<DonViResponse>>(
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
