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
  CAN_BO_API_ENDPOINT,
  CanBoDetailResponse,
  CanBoExportRequest,
  CanBoFilterRequest,
  CanBoFormRequest,
  CanBoImportResponseData,
  CanBoResponse,
} from '@app/model/admin/can-bo.model';

@Injectable({ providedIn: 'root' })
export class CanBoService {
  private readonly baseUrl = `${environment.host_api}/${CAN_BO_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: CanBoFilterRequest) {
    return this.http.post<IResponse<ITableResponse<CanBoResponse>>>(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<CanBoDetailResponse>>(
      `${this.baseUrl}/${id}`,
      {
        context: this.silentContext,
      }
    );
  }

  update(id: string | number, payload: CanBoFormRequest) {
    return this.http.put<IResponse<CanBoDetailResponse>>(
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

  export(payload: CanBoExportRequest): Observable<HttpResponse<Blob>> {
    const params = new HttpParams({
      fromObject: {
        exportType: payload.exportType ?? 'EXCEL',
      },
    });
    const { exportType: _exportType, ...body } = payload;

    return this.http.post(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.EXPORT}`,
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
  ): Observable<IResponse<CanBoImportResponseData>> {
    return this.http.post<IResponse<CanBoImportResponseData>>(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.IMPORT_EXCEL}`,
      payload,
      {
        params: { unitId },
      }
    );
  }

  downloadTemplate(unitId: string | number): Observable<HttpResponse<Blob>> {
    return this.http.post(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.EXCEL_TEMPLATE}`,
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
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.IMPORT_ERROR_FILE}/${encodeURIComponent(errorFileToken)}`,
      {
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  create(payload: CanBoFormRequest) {
    return this.http.post<IResponse<CanBoDetailResponse>>(
      `${this.baseUrl}`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  generateCode(unitId: string | number) {
    return this.http.get<IResponse<string>>(`${this.baseUrl}/generate-code`, {
      params: { unitId },
      context: this.silentContext,
    });
  }

  getByGrade(gradeId: string | number, unitId?: string | number) {
    const params: Record<string, string | number> = {};
    if (unitId !== undefined && unitId !== null && unitId !== '') {
      params['unitId'] = unitId;
    }

    return this.http.get<IResponse<CanBoResponse[]>>(
      `${this.baseUrl}/${CAN_BO_API_ENDPOINT.BY_GRADE}/${gradeId}`,
      {
        params,
        context: this.silentContext,
      }
    );
  }
}
