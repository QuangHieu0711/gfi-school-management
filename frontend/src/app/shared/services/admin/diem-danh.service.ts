import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';

import {
  DiemDanhHocSinhOption,
  DiemDanhItemSaveRequest,
  DiemDanhKhoiNhomItem,
  DiemDanhNgayResponse,
  DiemDanhThangResponse,
  DIEM_DANH_API_ENDPOINT,
  HOC_SINH_DIEM_DANH_API_ENDPOINT,
  LOP_KHOI_NHOM_API_ENDPOINT,
} from '@app/model/admin/diem-danh.model';

@Injectable({ providedIn: 'root' })
export class DiemDanhService {
  private readonly attendanceBaseUrl = `${environment.host_api}/${DIEM_DANH_API_ENDPOINT.BASE_PATH}`;
  private readonly classBaseUrl = `${environment.host_api}/${LOP_KHOI_NHOM_API_ENDPOINT.BASE_PATH}`;
  private readonly studentBaseUrl = `${environment.host_api}/${HOC_SINH_DIEM_DANH_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getGradeClassGroups(unitId: number | string, schoolYearId: number | string) {
    return this.http.get<IResponse<DiemDanhKhoiNhomItem[]>>(
      `${this.classBaseUrl}/${LOP_KHOI_NHOM_API_ENDPOINT.GRADE_CLASS_GROUPS}`,
      {
        params: { unitId, schoolYearId },
      }
    );
  }

  getStudentsByClassroom(classroomId: number | string) {
    return this.http.get<IResponse<DiemDanhHocSinhOption[]>>(
      `${this.studentBaseUrl}/${HOC_SINH_DIEM_DANH_API_ENDPOINT.BY_CLASSROOM}`,
      {
        params: { classroomId },
      }
    );
  }

  getDailySheet(
    classroomId: number | string,
    attendanceDate: string,
    sessionType: string
  ) {
    return this.http.get<IResponse<DiemDanhNgayResponse>>(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.DAILY_SHEET}`,
      {
        params: { classroomId, attendanceDate, sessionType },
      }
    );
  }

  getMonthlySheet(
    classroomId: number | string,
    month: string,
    sessionType: string
  ) {
    return this.http.get<IResponse<DiemDanhThangResponse>>(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.MONTHLY_SHEET}`,
      {
        params: { classroomId, month, sessionType },
      }
    );
  }

  saveAttendance(payload: DiemDanhItemSaveRequest) {
    return this.http.put<IResponse<unknown>>(this.attendanceBaseUrl, payload, {
      context: this.silentContext,
    });
  }

  saveAttendanceBulk(payload: DiemDanhItemSaveRequest[]) {
    return this.http.put<IResponse<unknown>>(
      `${this.attendanceBaseUrl}/bulk`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }
}
