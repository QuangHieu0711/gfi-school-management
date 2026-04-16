import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  STAFF_TRAINING_API_ENDPOINT,
  StaffTrainingFilterRequest,
  StaffTrainingFormRequest,
  StaffTrainingListResponse,
  StaffTrainingResponse,
} from '@app/model/admin/dao-tao-can-bo.model';

@Injectable({ providedIn: 'root' })
export class StaffTrainingService {
  private readonly baseUrl = `${environment.host_api}/${STAFF_TRAINING_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: StaffTrainingFilterRequest) {
    return this.http.post<IResponse<StaffTrainingListResponse>>(
      `${this.baseUrl}/${STAFF_TRAINING_API_ENDPOINT.FILTER}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<StaffTrainingResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  create(payload: StaffTrainingFormRequest) {
    return this.http.post<IResponse<StaffTrainingResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: StaffTrainingFormRequest) {
    return this.http.put<IResponse<StaffTrainingResponse>>(
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
