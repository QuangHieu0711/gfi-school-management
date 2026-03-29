import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import { map, Observable } from 'rxjs';

import {
  VaiTroFilterRequest,
  VaiTroFormRequest,
  VaiTroResponse,
  VaiTroSearchResponse,
} from '@app/model/admin/vai-tro.model';

@Injectable({ providedIn: 'root' })
export class VaiTroService {
  private readonly baseUrl = `${environment.host_api}/roles`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(
    payload: VaiTroFilterRequest
  ): Observable<IResponse<ITableResponse<VaiTroResponse>>> {
    return this.http
      .post<IResponse<VaiTroSearchResponse>>(`${this.baseUrl}/search`, payload)
      .pipe(
        map((response) => ({
          ...response,
          data: this.normalizeTableData(response.data),
        }))
      );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<VaiTroResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  create(payload: VaiTroFormRequest) {
    return this.http.post<IResponse<VaiTroResponse>>(this.baseUrl, payload, {
      context: this.silentContext,
    });
  }

  update(id: string | number, payload: VaiTroFormRequest) {
    return this.http.put<IResponse<VaiTroResponse>>(`${this.baseUrl}/${id}`, payload, {
      context: this.silentContext,
    });
  }

  delete(id: string | number) {
    return this.http.delete<IResponse<null>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  private normalizeTableData(data?: VaiTroSearchResponse): ITableResponse<VaiTroResponse> {
    const items = data?.items ?? data?.content ?? data?.data ?? [];

    const recordTotal = Number(
      data?.recordTotal ?? data?.totalCount ?? data?.totalElements ?? items.length
    );

    const pageSize = Number(data?.pageSize ?? items.length ?? 0);
    const pageNo = Number(data?.pageNo ?? data?.pageNow ?? data?.number ?? 0);

    return {
      pageNo,
      pageSize,
      totalCount: recordTotal,
      totalPage: Number(data?.totalPage ?? data?.pageTotal ?? 1),
      data: items,
      items,
      recordTotal,
      pageTotal: Number(data?.pageTotal ?? data?.totalPage ?? 1),
    };
  }
}
