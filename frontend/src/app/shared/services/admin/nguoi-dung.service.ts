import {
  HttpClient,
  HttpContext,
  HttpParams,
  HttpResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import {
  NGUOI_DUNG_API_ENDPOINT,
  NguoiDungExportRequest,
  NguoiDungFilterRequest,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { ID_TYPE, IResponse, ITableResponse } from '@model/response.model';
import { Observable, of } from 'rxjs';
import { DonViOptionResponse } from '@app/model/admin/don-vi.model';

@Injectable({ providedIn: 'root' })
export class NguoiDungService {
  private readonly baseUrl = `${environment.host_api}/${NGUOI_DUNG_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) { }

  filter(
    payload: NguoiDungFilterRequest
  ): Observable<IResponse<ITableResponse<NguoiDungResponse>>> {
    return this.http.post<IResponse<ITableResponse<NguoiDungResponse>>>(
      `${this.baseUrl}/${NGUOI_DUNG_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: ID_TYPE) {
    return this.http.get<IResponse<NguoiDungResponse>>(
      `${this.baseUrl}/${id}`,
      {
        context: this.silentContext,
      }
    );
  }

  getCreateUserRoleOptions() {
    return this.http.get<IResponse<DonViOptionResponse[]>>(
      `${this.baseUrl}/${NGUOI_DUNG_API_ENDPOINT.ROLE}`
    );
  }

  getStaffOptions() {
    return this.http.get<IResponse<any[]>>(
      `${this.baseUrl}/${NGUOI_DUNG_API_ENDPOINT.STAFF_OPTIONS}`
    );
  }

  create(payload: NguoiDungFormRequest) {
    return this.http.post<IResponse<NguoiDungResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(payload: NguoiDungFormRequest) {
    return this.http.put<IResponse<NguoiDungResponse>>(
      `${this.baseUrl}/${payload.id}`,
      this.omitId(payload),
      {
        context: this.silentContext,
      }
    );
  }

  delete(id: ID_TYPE) {
    return this.http.delete<IResponse<null>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  export(payload: NguoiDungExportRequest): Observable<HttpResponse<Blob>> {
    const params = new HttpParams({
      fromObject: {
        exportType: payload.exportType ?? 'EXCEL',
      },
    });

    return this.http.post(
      `${this.baseUrl}/${NGUOI_DUNG_API_ENDPOINT.EXPORT}`,
      payload,
      {
        params,
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  private omitId(payload: NguoiDungFormRequest) {
    const rest = { ...payload };
    delete rest.id;
    return rest;
  }
}
