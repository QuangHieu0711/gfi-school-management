import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse, ITableResponse } from '@model/response.model';
import {
  PHAN_CONG_GIANG_DAY_API_ENDPOINT,
  PhanCongGiangDayDetailRequest,
  PhanCongGiangDayDetailResponse,
  PhanCongGiangDayFilterRequest,
  PhanCongGiangDayResponse,
  PhanCongGiangDayUpsertRequest,
} from '@app/model/admin/phan-cong-giang-day.model';

@Injectable({ providedIn: 'root' })
export class PhanCongGiangDayService {
  private readonly baseUrl = `${environment.host_api}/${PHAN_CONG_GIANG_DAY_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  filter(payload: PhanCongGiangDayFilterRequest) {
    return this.http.post<IResponse<ITableResponse<PhanCongGiangDayResponse>>>(
      `${this.baseUrl}/${PHAN_CONG_GIANG_DAY_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getDetail(payload: PhanCongGiangDayDetailRequest) {
    return this.http.post<IResponse<PhanCongGiangDayDetailResponse>>(
      `${this.baseUrl}/${PHAN_CONG_GIANG_DAY_API_ENDPOINT.DETAIL}`,
      payload
    );
  }

  create(payload: PhanCongGiangDayUpsertRequest) {
    return this.http.post<IResponse<PhanCongGiangDayResponse>>(
      this.baseUrl,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  update(payload: PhanCongGiangDayUpsertRequest) {
    return this.http.put<IResponse<PhanCongGiangDayResponse>>(
      this.baseUrl,
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
