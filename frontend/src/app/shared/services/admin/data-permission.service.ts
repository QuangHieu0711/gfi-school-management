import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { ID_TYPE, IResponse } from '@model/response.model';

import {
  DATA_PERMISSION_API_ENDPOINT,
  DataPermissionBulkFormRequest,
  DataPermissionResponse,
} from '@app/model/admin/data-permission.model';

@Injectable({ providedIn: 'root' })
export class DataPermissionService {
  private readonly baseUrl = `${environment.host_api}/${DATA_PERMISSION_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getByRoleId(roleId: ID_TYPE) {
    return this.http.get<IResponse<DataPermissionResponse[]>>(
      `${this.baseUrl}/${roleId}`
    );
  }

  save(payload: DataPermissionBulkFormRequest) {
    return this.http.post<IResponse<DataPermissionResponse[]>>(
      `${this.baseUrl}/${DATA_PERMISSION_API_ENDPOINT.SAVE}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }
}
