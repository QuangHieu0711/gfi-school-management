/* eslint-disable @typescript-eslint/no-explicit-any */
import { Injectable } from '@angular/core';
import {
  NGUOI_DUNG_API_ENDPOINT,
  NguoiDungFormRequest,
  NguoiDungResponse,
} from '@app/model/admin/nguoi-dung.model';
import { environment } from '@env/environment';
import { HttpOptions, ID_TYPE, ITableResponse } from '@model/response.model';
import { ApiService } from '@service';

@Injectable({ providedIn: 'root' })
export class NguoiDungService {
  basePath = NGUOI_DUNG_API_ENDPOINT.BASE_PATH;
  private readonly routerHost = environment.sys;

  constructor(private readonly apiService: ApiService) {}

  filter(payload: any) {
    return this.apiService.post<ITableResponse<NguoiDungResponse>>(
      `${this.basePath}/${NGUOI_DUNG_API_ENDPOINT.FILTER}`,
      { ...payload, pageNow: payload.pageNow + 1 },
      undefined,
      {
        routerHost: this.routerHost,
      }
    );
  }

  getById(id: ID_TYPE) {
    return this.apiService.get<NguoiDungResponse>(
      `${this.basePath}/${NGUOI_DUNG_API_ENDPOINT.GET_BY_ID}`,
      {
        params: { id },
        routerHost: this.routerHost,
      }
    );
  }

  delete(ids: ID_TYPE[]) {
    return this.apiService.deleteBatch<ITableResponse<NguoiDungResponse>>(
      `${this.basePath}`,
      ids,
      { routerHost: this.routerHost }
    );
  }

  create(payload: NguoiDungFormRequest, options?: HttpOptions) {
    return this.apiService.post(
      `${this.basePath}/${NGUOI_DUNG_API_ENDPOINT.CREATE}`,
      payload,
      undefined,
      { ...options, routerHost: this.routerHost }
    );
  }

  update(payload: NguoiDungFormRequest, options?: HttpOptions) {
    return this.apiService.put(
      `${this.basePath}/${NGUOI_DUNG_API_ENDPOINT.UPDATE}`,
      payload,
      { ...options, routerHost: this.routerHost }
    );
  }

  filterAll() {
    return this.apiService.get<NguoiDungResponse[]>(
      `${this.basePath}/${NGUOI_DUNG_API_ENDPOINT.FILTER_ALL}`,
      { routerHost: this.routerHost }
    );
  }

  export(payload: unknown) {
    return this.apiService.post<Blob>(
      `${this.basePath}/export`,
      payload,
      {},
      {
        responseType: 'blob',
        observe: 'response',
        routerHost: this.routerHost,
      }
    );
  }

  import(payload: FormData) {
    return this.apiService.post<{ message: string }>(
      `${this.basePath}/import`,
      payload,
      undefined,
      {
        routerHost: this.routerHost,
        headers: {
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
      }
    );
  }
  downloadTemplate() {
    return this.apiService.get<Blob>(`${this.basePath}/download-template`, {
      responseType: 'blob',
      routerHost: this.routerHost,
    });
  }
}
