import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse, ID_TYPE } from '@model/response.model';

import {
  PERMISSION_API_ENDPOINT,
  PermissionFilterRequest,
  PermissionFormRequest,
  PermissionResponse,
} from '@app/model/admin/permission.model';

@Injectable({ providedIn: 'root' })
export class PermissionAdminService {
  private readonly baseUrl = `${environment.host_api}/${PERMISSION_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: PermissionFilterRequest) {
    return this.http.post<
      IResponse<ITableResponse<PermissionResponse> | PermissionResponse[]>
    >(`${this.baseUrl}/${PERMISSION_API_ENDPOINT.FILTER}`, payload);
  }

  create(payload: PermissionFormRequest) {
    return this.http.post<IResponse<PermissionResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: ID_TYPE, payload: PermissionFormRequest) {
    return this.http.put<IResponse<PermissionResponse>>(
      `${this.baseUrl}/${id}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }
}
