import {
  HttpClient,
  HttpContext,
  HttpParams,
  HttpResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import { Observable } from 'rxjs';

import {
  NAM_HOC_API_ENDPOINT,
  NamHocExportRequest,
  NamHocFilterRequest,
  NamHocFormRequest,
  NamHocImportResponseData,
  NamHocOptionResponse,
  NamHocResponse,
} from '@app/model/admin/nam-hoc.model';

@Injectable({ providedIn: 'root' })
export class NamHocService {
  private readonly baseUrl = `${environment.host_api}/${NAM_HOC_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: NamHocFilterRequest
  ): Observable<IResponse<ITableResponse<NamHocResponse>>> {
    return this.http.post<IResponse<ITableResponse<NamHocResponse>>>(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<NamHocResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  getOptions() {
    return this.http.get<IResponse<NamHocOptionResponse[]>>(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.OPTIONS}`
    );
  }

  getCurrent() {
    return this.http.get<IResponse<NamHocOptionResponse>>(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.CURRENT}`
    );
  }

  create(payload: NamHocFormRequest) {
    return this.http.post<IResponse<NamHocResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: NamHocFormRequest) {
    return this.http.put<IResponse<NamHocResponse>>(
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

  export(payload: NamHocExportRequest): Observable<HttpResponse<Blob>> {
    const params = new HttpParams({
      fromObject: {
        exportType: payload.exportType ?? 'EXCEL',
      },
    });
    const { exportType: _exportType, ...body } = payload;

    return this.http.post(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.EXPORT}`,
      body,
      {
        params,
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  import(payload: FormData): Observable<IResponse<NamHocImportResponseData>> {
    return this.http.post<IResponse<NamHocImportResponseData>>(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.IMPORT_EXCEL}`,
      payload
    );
  }

  downloadImportErrorFile(
    errorFileToken: string
  ): Observable<HttpResponse<Blob>> {
    return this.http.get(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.IMPORT_ERROR_FILE}/${encodeURIComponent(errorFileToken)}`,
      {
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.post(
      `${this.baseUrl}/${NAM_HOC_API_ENDPOINT.EXCEL_TEMPLATE}`,
      {},
      {
        responseType: 'blob',
      }
    );
  }
}
