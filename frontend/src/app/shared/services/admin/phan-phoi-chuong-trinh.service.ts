import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';

import {
  PHAN_PHOI_CHUONG_TRINH_API_ENDPOINT,
  PhanPhoiChuongTrinhFilterRequest,
  PhanPhoiChuongTrinhFormRequest,
  PhanPhoiChuongTrinhImportRequest,
  PhanPhoiChuongTrinhResponse,
  PhanPhoiChuongTrinhUpdateRequest,
} from '@app/model/admin/phan-phoi-chuong-trinh.model';

@Injectable({ providedIn: 'root' })
export class PhanPhoiChuongTrinhService {
  private readonly baseUrl = `${environment.host_api}/${PHAN_PHOI_CHUONG_TRINH_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: PhanPhoiChuongTrinhFilterRequest) {
    return this.http.post<IResponse<ITableResponse<PhanPhoiChuongTrinhResponse>>>(
      `${this.baseUrl}/${PHAN_PHOI_CHUONG_TRINH_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<PhanPhoiChuongTrinhResponse>>(
      `${this.baseUrl}/${id}`,
      {
        context: this.silentContext,
      }
    );
  }

  create(payload: PhanPhoiChuongTrinhFormRequest) {
    return this.http.post<IResponse<PhanPhoiChuongTrinhResponse>>(
      this.baseUrl,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  update(id: string | number, payload: PhanPhoiChuongTrinhUpdateRequest) {
    return this.http.put<IResponse<PhanPhoiChuongTrinhResponse>>(
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

  downloadTemplate(params: PhanPhoiChuongTrinhImportRequest) {
    return this.http.post(`${this.baseUrl}/${PHAN_PHOI_CHUONG_TRINH_API_ENDPOINT.EXCEL_TEMPLATE}`, null, {
      params: this.buildImportQueryParams(params),
      observe: 'response',
      responseType: 'blob',
    });
  }

  importExcel(params: PhanPhoiChuongTrinhImportRequest, file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.http.post<IResponse<null>>(
      `${this.baseUrl}/${PHAN_PHOI_CHUONG_TRINH_API_ENDPOINT.IMPORT_EXCEL}`,
      formData,
      {
        params: this.buildImportQueryParams(params),
        context: this.silentContext,
      }
    );
  }

  private buildImportQueryParams(params: PhanPhoiChuongTrinhImportRequest) {
    return {
      schoolYearId: String(params.schoolYearId),
      unitId: String(params.unitId),
      classroomId: String(params.classroomId),
      subjectId: String(params.subjectId),
    };
  }
}
