import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { IResponse, ID_TYPE } from '@model/response.model';

import {
  PHAN_CONG_GIAO_VIEN_API_ENDPOINT,
  PhanCongGiaoVienClassroomQueryParams,
  PhanCongGiaoVienClassroomResponse,
} from '@app/model/admin/phan-cong-giao-vien.model';

@Injectable({ providedIn: 'root' })
export class PhanCongGiaoVienService {
  private readonly baseUrl = `${environment.host_api}/${PHAN_CONG_GIAO_VIEN_API_ENDPOINT.SUBJECTS}`;

  constructor(private readonly http: HttpClient) {}

  getClassroomsBySubject(
    subjectId: ID_TYPE,
    params?: PhanCongGiaoVienClassroomQueryParams
  ) {
    const queryParams: Record<string, string | number> = {};

    if (
      params?.unitId !== undefined &&
      params.unitId !== null &&
      params.unitId !== ''
    ) {
      queryParams['unitId'] = params.unitId;
    }

    return this.http.get<IResponse<PhanCongGiaoVienClassroomResponse[]>>(
      `${this.baseUrl}/${subjectId}/${PHAN_CONG_GIAO_VIEN_API_ENDPOINT.CLASSROOMS}`,
      { params: queryParams }
    );
  }
}
