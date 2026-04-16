import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  STAFF_FOREIGN_LANGUAGE_API_ENDPOINT,
  StaffForeignLanguageFilterRequest,
  StaffForeignLanguageFormRequest,
  StaffForeignLanguageListResponse,
  StaffForeignLanguageResponse,
} from '@app/model/admin/thong-tin-ngoai-ngu-can-bo.model';

@Injectable({ providedIn: 'root' })
export class StaffForeignLanguageService {
  private readonly baseUrl = `${environment.host_api}/${STAFF_FOREIGN_LANGUAGE_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: StaffForeignLanguageFilterRequest) {
    return this.http.post<IResponse<StaffForeignLanguageListResponse>>(
      `${this.baseUrl}/${STAFF_FOREIGN_LANGUAGE_API_ENDPOINT.FILTER}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<StaffForeignLanguageResponse>>(
      `${this.baseUrl}/${id}`,
      {
        context: this.silentContext,
      }
    );
  }

  create(payload: StaffForeignLanguageFormRequest) {
    return this.http.post<IResponse<StaffForeignLanguageResponse>>(
      this.baseUrl,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  update(id: string | number, payload: StaffForeignLanguageFormRequest) {
    return this.http.put<IResponse<StaffForeignLanguageResponse>>(
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
