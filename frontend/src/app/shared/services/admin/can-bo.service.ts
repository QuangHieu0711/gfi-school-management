import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  CAN_BO_API_ENDPOINT,
  CanBoFilterRequest,
  CanBoResponse,
} from '@app/model/admin/can-bo.model';

@Injectable({ providedIn: 'root' })
export class CanBoService {
  private readonly baseUrl = `${environment.host_api}/${CAN_BO_API_ENDPOINT.BASE_PATH}`;

  constructor(private readonly http: HttpClient) {}

  filter(payload: CanBoFilterRequest) {
    return this.http.post<IResponse<ITableResponse<CanBoResponse>>>(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.FILTER}`,
      payload
    );
  }
}
