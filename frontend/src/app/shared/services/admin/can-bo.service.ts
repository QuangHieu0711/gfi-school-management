import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  CAN_BO_API_ENDPOINT,
  CanBoDetailResponse,
  CanBoFilterRequest,
  CanBoFormRequest,
  CanBoResponse,
} from '@app/model/admin/can-bo.model';

@Injectable({ providedIn: 'root' })
export class CanBoService {
  private readonly baseUrl = `${environment.host_api}/${CAN_BO_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: CanBoFilterRequest) {
    return this.http.post<IResponse<ITableResponse<CanBoResponse>>>(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<CanBoDetailResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: CanBoFormRequest) {
    return this.http.put<IResponse<CanBoDetailResponse>>(
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

  create(payload: CanBoFormRequest) {
    return this.http.post<IResponse<CanBoDetailResponse>>(
      `${this.baseUrl}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  generateCode(unitId: string | number) {
    return this.http.get<IResponse<string>>(`${this.baseUrl}/generate-code`, {
      params: { unitId },
      context: this.silentContext,
    });
  }
}
