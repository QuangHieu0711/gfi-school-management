import { HttpClient, HttpContext, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { Observable } from 'rxjs';

export interface DanhMucHcmItem {
  Ma: string;
  MoTa?: string;
  Ten: string;
  Used?: boolean;
}

export interface DanhMucHcmResponse<T> {
  StatusCode: number;
  Description: string | null;
  ResultObject: T;
  ResultType: string;
  Status: string;
  ThrowException: boolean;
}

@Injectable({ providedIn: 'root' })
export class DanhMucHcmService {
  private readonly baseUrl = '/hcmesb-test';
  private readonly authorizationToken =
    'eyJhcHAiOiJUUEhDTSIsInNlY3JldCI6IkRXa1FnWTFZU1MiLCJrZXkiOiJyVGtoWUNCd0hNIn0=';
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });

  constructor(private readonly http: HttpClient) {}

  getDanToc(): Observable<DanhMucHcmResponse<DanhMucHcmItem[]>> {
    return this.http.get<DanhMucHcmResponse<DanhMucHcmItem[]>>(
      `${this.baseUrl}/GetDanhMucDanToc`,
      {
        headers: new HttpHeaders({
          Authorization: this.authorizationToken,
        }),
        context: this.silentContext,
      }
    );
  }
}
