import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  LOP_MON_HOC_API_ENDPOINT,
  LopMonHocAssignRequest,
  LopMonHocDetailResponse,
} from '@app/model/admin/lop-mon-hoc.model';

@Injectable({ providedIn: 'root' })
export class LopMonHocService {
  private readonly baseUrl = `${environment.host_api}/${LOP_MON_HOC_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getDetail(classroomId: number | string) {
    return this.http.get<IResponse<LopMonHocDetailResponse>>(
      `${this.baseUrl}/${classroomId}`,
      {
        context: this.silentContext,
      }
    );
  }

  assign(payload: LopMonHocAssignRequest) {
    return this.http.post<IResponse<null>>(
      `${this.baseUrl}/${LOP_MON_HOC_API_ENDPOINT.ASSIGN}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }
}
