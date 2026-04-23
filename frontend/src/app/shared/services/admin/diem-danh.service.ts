import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';
import { Observable } from 'rxjs';

import {
  DiemDanhBulkSaveRequest,
  DiemDanhExportRequest,
  DiemDanhHocSinhOption,
  DiemDanhImportRequest,
  DiemDanhImportResponseData,
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
    const [year, monthValue] = `${month}`.split('-');

    return this.http.get<IResponse<DiemDanhThangResponse>>(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.MONTHLY_SHEET}`,
      {
        params: {
          classroomId,
          year: Number(year),
          month: Number(monthValue),
          sessionType,
        },
      }
    );
  }

  saveAttendance(payload: DiemDanhItemSaveRequest) {
    return this.http.put<IResponse<unknown>>(this.attendanceBaseUrl, payload, {
      context: this.silentContext,
    });
  }

  saveAttendanceBulk(payload: DiemDanhBulkSaveRequest) {
    return this.http.put<IResponse<unknown>>(
      `${this.attendanceBaseUrl}/bulk`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  export(payload: DiemDanhExportRequest): Observable<HttpResponse<Blob>> {
    const { classroomId, year, month, sessionType, exportType } = payload;

    return this.http.get(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.EXPORT}`,
      {
        params: {
          classroomId,
          year,
          month,
          sessionType,
          exportType,
        },
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  importExcel(params: DiemDanhImportRequest, file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.http.post<IResponse<DiemDanhImportResponseData>>(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.IMPORT_EXCEL}`,
      formData,
      {
        params: {
          classroomId: String(params.classroomId),
          year: String(params.year),
          month: String(params.month),
          sessionType: params.sessionType,
        },
        context: this.silentContext,
      }
    );
  }

  downloadTemplate(
    params: DiemDanhImportRequest
  ): Observable<HttpResponse<Blob>> {
    return this.http.post(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.EXCEL_TEMPLATE}`,
      null,
      {
        params: {
          classroomId: String(params.classroomId),
          year: String(params.year),
          month: String(params.month),
          sessionType: params.sessionType,
        },
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  downloadImportErrorFile(token: string): Observable<HttpResponse<Blob>> {
    return this.http.get(
      `${this.attendanceBaseUrl}/${DIEM_DANH_API_ENDPOINT.IMPORT_ERROR_FILE}/${encodeURIComponent(token)}`,
      {
        observe: 'response',
        responseType: 'blob',
      }
    );
  }
}
