/* eslint-disable @typescript-eslint/no-explicit-any */
import { HttpClient, HttpContext, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import {
  NGUOI_DUNG_API_ENDPOINT,
  NguoiDungFilterRequest,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { HttpOptions, ID_TYPE, IResponse, ITableResponse } from '@model/response.model';
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NguoiDungService {
  private readonly baseUrl = `${environment.host_api}/${NGUOI_DUNG_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: NguoiDungFilterRequest
  ): Observable<IResponse<ITableResponse<NguoiDungResponse>>> {
    return this.http.post<IResponse<ITableResponse<NguoiDungResponse>>>(
      `${this.baseUrl}/${NGUOI_DUNG_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: ID_TYPE) {
    return this.http.get<IResponse<NguoiDungResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  create(payload: NguoiDungFormRequest, _options?: HttpOptions) {
    return this.http.post<IResponse<NguoiDungResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(payload: NguoiDungFormRequest, _options?: HttpOptions) {
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

  export(payload: {
    exportType?: 'PDF' | 'EXCEL';
  }): Observable<HttpResponse<Blob>> {
    const content =
      payload.exportType === 'PDF'
        ? 'DANH SACH NGUOI DUNG'
        : 'ID,HoTen,TenTaiKhoan,Email,DonVi,TrangThai,NhomQuyen\n';
    const blob = new Blob([content], {
      type:
        payload.exportType === 'PDF'
          ? 'application/pdf'
          : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const extension = payload.exportType === 'PDF' ? 'pdf' : 'xlsx';

    return of(
      new HttpResponse({
        body: blob,
        headers: new HttpHeaders({
          'content-disposition': `attachment; filename="nguoi-dung.${extension}"`,
        }),
      })
    );
  }

  import(payload: FormData): Observable<{ data: { message: string } }> {
    const file = payload.get('file');

    return of({
      data: {
        message:
          file instanceof File
            ? `Imported locally: ${file.name}`
            : 'Imported locally',
      },
    });
  }

  downloadTemplate(): Observable<Blob> {
    const template =
      'Ho,Ten,TenTaiKhoan,Email,SoDienThoai,DonVi,TrangThai,NhomQuyen\n';

    return of(
      new Blob([template], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
    );
  }

  private omitId(payload: NguoiDungFormRequest) {
    const { id, ...rest } = payload;
    return rest;
  }
}
