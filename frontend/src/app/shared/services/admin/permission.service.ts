import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ID_TYPE } from '@model/response.model';

import {
  PERMISSION_API_ENDPOINT,
  PermissionBulkFormRequest,
  PermissionResponse,
} from '@app/model/admin/permission.model';

@Injectable({ providedIn: 'root' })
export class PermissionAdminService {
  private readonly baseUrl = `${environment.host_api}/${PERMISSION_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getByRoleId(roleId: ID_TYPE) {
    return this.http.get<IResponse<PermissionResponse[]>>(
      `${this.baseUrl}/${roleId}`
    );
  }

  save(payload: PermissionBulkFormRequest) {
    return this.http.post<IResponse<PermissionResponse[]>>(
      `${this.baseUrl}/save`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }
}
