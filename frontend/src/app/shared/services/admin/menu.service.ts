import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ERROR_CONTEXT } from '@constant/error.registry';
import { environment } from '@env/environment';
import { IResponse } from '@model/response.model';
import { Observable, Subject, shareReplay, tap } from 'rxjs';

import {
  MENU_API_ENDPOINT,
  MenuFilterRequest,
  MenuFormRequest,
  MenuOptionResponse,
  MenuResponse,
  MenuExportRequest,
} from '@app/model/admin/menu.model';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly baseUrl = `${environment.host_api}/${MENU_API_ENDPOINT.BASE_PATH}`;
  private readonly silentContext = new HttpContext().set(ERROR_CONTEXT, {
    silent: true,
  });
  private optionsRequest$?: Observable<IResponse<MenuOptionResponse[]>>;
  private readonly menuChangedSource = new Subject<void>();
  readonly menuChanged$ = this.menuChangedSource.asObservable();

  constructor(private readonly http: HttpClient) {}

  filter(payload: MenuFilterRequest) {
    return this.http.post<IResponse<MenuResponse[]>>(
      `${this.baseUrl}/${MENU_API_ENDPOINT.FILTER}`,
      payload
    );
  }

  getById(id: string | number) {
    return this.http.get<IResponse<MenuResponse>>(`${this.baseUrl}/${id}`, {
      context: this.silentContext,
    });
  }

  getOptions() {
    if (!this.optionsRequest$) {
      this.optionsRequest$ = this.http
        .get<IResponse<MenuOptionResponse[]>>(
          `${this.baseUrl}/${MENU_API_ENDPOINT.OPTIONS}`
        )
        .pipe(shareReplay(1));
    }

    return this.optionsRequest$;
  }

  getOptionsFresh() {
    this.clearOptionsCache();
    return this.getOptions();
  }

  create(payload: MenuFormRequest) {
    return this.http
      .post<IResponse<MenuResponse>>(this.baseUrl, payload, {
        context: this.silentContext,
      })
      .pipe(
        tap(() => {
          this.clearOptionsCache();
          this.notifyMenuChanged();
        })
      );
  }

  update(id: string | number, payload: MenuFormRequest) {
    return this.http
      .put<IResponse<MenuResponse>>(`${this.baseUrl}/${id}`, payload, {
        context: this.silentContext,
      })
      .pipe(
        tap(() => {
          this.clearOptionsCache();
          this.notifyMenuChanged();
        })
      );
  }

  delete(id: string | number) {
    return this.http
      .delete<IResponse<null>>(`${this.baseUrl}/${id}`, {
        context: this.silentContext,
      })
      .pipe(
        tap(() => {
          this.clearOptionsCache();
          this.notifyMenuChanged();
        })
      );
  }

  export(payload: MenuExportRequest): Observable<any> {
    const params = new HttpParams({
      fromObject: {
        exportType: payload.exportType ?? 'EXCEL',
      },
    });
    const { exportType: _exportType, ...body } = payload;

    return this.http.post(
      `${this.baseUrl}/${MENU_API_ENDPOINT.EXPORT}`,
      body,
      {
        params,
        observe: 'response',
        responseType: 'blob',
      }
    );
  }

  clearOptionsCache() {
    this.optionsRequest$ = undefined;
  }

  notifyMenuChanged() {
    this.menuChangedSource.next();
  }
}
