import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';
import {
  EvaluationBulkGenerateCommentRequest,
  EvaluationBulkSaveRequest,
  EvaluationEditWindowRequest,
  EvaluationEditWindowResponse,
  EvaluationGenerateCommentRequest,
  EvaluationSheetResponse,
} from '@app/model/admin/evaluation.model';

@Injectable({ providedIn: 'root' })
export class EvaluationService {
  private readonly baseUrl = `${environment.host_api}/evaluations`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  saveBulk(payload: EvaluationBulkSaveRequest) {
    return this.http.put<IResponse<unknown>>(`${this.baseUrl}/bulk`, payload, {
      context: this.silentContext,
    });
  }

  generateComment(payload: EvaluationGenerateCommentRequest) {
    return this.http.post<IResponse<string>>(`${this.baseUrl}/generate-comment`, payload, {
      context: this.silentContext,
    });
  }

  bulkGenerateComment(payload: EvaluationBulkGenerateCommentRequest) {
    return this.http.post<IResponse<{ [key: string]: string }>>(`${this.baseUrl}/bulk-generate-comment`, payload, {
      context: this.silentContext,
    });
  }

  getSheet(classroomId: string | number, subjectId: string | number, semesterId: string | number) {
    return this.http.get<IResponse<EvaluationSheetResponse>>(`${this.baseUrl}/sheet`, {
      params: { classroomId, subjectId, semesterId },
    });
  }

  getEditWindow(semesterId: string | number) {
    return this.http.get<IResponse<EvaluationEditWindowResponse | null>>(
      `${this.baseUrl}/edit-window`,
      {
        params: { semesterId },
        context: this.silentContext,
      }
    );
  }

  saveEditWindow(payload: EvaluationEditWindowRequest) {
    return this.http.put<IResponse<EvaluationEditWindowResponse>>(
      `${this.baseUrl}/edit-window`,
      payload,
      {
        context: this.silentContext,
      }
    );
  }

  exportTemplate(classroomId: number, subjectId: number, semesterId: number) {
    return this.http.get(`${this.baseUrl}/export-template`, {
      params: { classroomId, subjectId, semesterId },
      responseType: 'blob',
    });
  }

  importExcel(file: File, classroomId: number, subjectId: number, semesterId: number) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('classroomId', classroomId.toString());
    formData.append('subjectId', subjectId.toString());
    formData.append('semesterId', semesterId.toString());

    return this.http.post<IResponse<any>>(`${this.baseUrl}/import`, formData);
  }

  downloadImportError(token: string) {
    return this.http.get(`${this.baseUrl}/import-errors/${token}`, {
      responseType: 'blob',
    });
  }
}
