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
  HOC_SINH_API_ENDPOINT,
  HocSinhDetailResponse,
  HocSinhExportRequest,
  HocSinhFilterRequest,
  HocSinhFormRequest,
  HocSinhImportResponseData,
  HocSinhResponse,
} from '@app/model/admin/hoc-sinh.model';

@Injectable({ providedIn: 'root' })
export class HocSinhService {
  private readonly baseUrl = `${environment.host_api}/${HOC_SINH_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: HocSinhFilterRequest) {
    return this.http.post<IResponse<ITableResponse<HocSinhResponse>>>(
      `${this.baseUrl}/${HOC_SINH_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<HocSinhDetailResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  create(payload: HocSinhFormRequest) {
    return this.http.post<IResponse<HocSinhDetailResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: HocSinhFormRequest) {
    return this.http.put<IResponse<HocSinhDetailResponse>>(
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

  export(payload: HocSinhExportRequest): Observable<HttpResponse<Blob>> {
    const params = new HttpParams({
      fromObject: {
        exportType: payload.exportType ?? 'EXCEL',
      },
    });
    const { exportType: _exportType, ...body } = payload;

    return this.http.post(
      `${this.baseUrl}/${HOC_SINH_API_ENDPOINT.EXPORT}`,
      body,
      {
        params,
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  import(
    unitId: string | number,
    payload: FormData
  ): Observable<IResponse<HocSinhImportResponseData>> {
    return this.http.post<IResponse<HocSinhImportResponseData>>(
      `${this.baseUrl}/${HOC_SINH_API_ENDPOINT.IMPORT_EXCEL}`,
      payload,
      {
        params: { unitId },
      }
    );
  }

  downloadTemplate(unitId: string | number): Observable<HttpResponse<Blob>> {
    return this.http.post(
      `${this.baseUrl}/${HOC_SINH_API_ENDPOINT.EXCEL_TEMPLATE}`,
      {},
      {
        params: { unitId },
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  downloadImportErrorFile(errorFileToken: string): Observable<HttpResponse<Blob>> {
    return this.http.get(
      `${this.baseUrl}/${HOC_SINH_API_ENDPOINT.IMPORT_ERROR_FILE}/${encodeURIComponent(errorFileToken)}`,
      {
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  generateCode(unitId: string | number) {
    return this.http.get<IResponse<string>>(`${this.baseUrl}/generate-code`, {
      params: { unitId },
      context: this.silentContext,
    });
  }
}
