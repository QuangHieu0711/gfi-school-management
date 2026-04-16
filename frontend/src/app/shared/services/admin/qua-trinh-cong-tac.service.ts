import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  STAFF_JOB_HISTORY_API_ENDPOINT,
  StaffJobHistoryFilterRequest,
  StaffJobHistoryFormRequest,
  StaffJobHistoryListResponse,
  StaffJobHistoryResponse,
} from '@app/model/admin/qua-trinh-cong-tac.model';

@Injectable({ providedIn: 'root' })
export class StaffJobHistoryService {
  private readonly baseUrl = `${environment.host_api}/${STAFF_JOB_HISTORY_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: StaffJobHistoryFilterRequest) {
    return this.http.post<IResponse<StaffJobHistoryListResponse>>(
      `${this.baseUrl}/${STAFF_JOB_HISTORY_API_ENDPOINT.FILTER}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<StaffJobHistoryResponse>>(
      `${this.baseUrl}/${id}`,
      {
        context: this.silentContext,
      }
    );
  }

  create(payload: StaffJobHistoryFormRequest) {
    return this.http.post<IResponse<StaffJobHistoryResponse>>(
      this.baseUrl,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  update(id: string | number, payload: StaffJobHistoryFormRequest) {
    return this.http.put<IResponse<StaffJobHistoryResponse>>(
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
