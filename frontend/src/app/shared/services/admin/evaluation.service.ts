import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';
import { EvaluationBulkSaveRequest } from '@app/model/admin/evaluation.model';

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
}
