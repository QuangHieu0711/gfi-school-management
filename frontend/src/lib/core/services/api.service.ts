import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { HttpOptions, ID_TYPE, IResponse } from '@model/response.model';

import { LoadingService } from './loading.service';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(private readonly loadingService: LoadingService) {}

  get<T>(_endpoint: string, _options?: HttpOptions): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  getById<T>(
    _endpoint: string,
    _id: ID_TYPE,
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  post<T>(
    _endpoint: string,
    _data: unknown,
    _queryParams?: Record<string, string | number | boolean>,
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  put<T>(
    _endpoint: string,
    _data: unknown,
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  delete<T>(
    _endpoint: string,
    _queryParams: Record<string, string | number | boolean>,
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  deleteBatch<T>(
    _endpoint: string,
    _ids: ID_TYPE[],
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  postFile<T>(
    _endpoint: string,
    _data: unknown,
    _queryParams?: Record<string, string | number | boolean>,
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  putFile<T>(
    _endpoint: string,
    _data: FormData,
    _queryParams?: Record<string, string | number | boolean>,
    _options?: HttpOptions
  ): Observable<IResponse<T>> {
    return this.disabledCall();
  }

  download(_endpoint: string, _options?: HttpOptions): Observable<Blob> {
    return this.disabledCall();
  }

  private disabledCall<T>(): Observable<T> {
    this.loadingService.hide();
    return throwError(
      () => new Error('ApiService has been disabled. Recreate your API layer before using it.')
    );
  }
}
