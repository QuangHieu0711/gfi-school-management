import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpContext,
  HttpHeaders,
  HttpParams,
} from '@angular/common/http';
import { finalize, Observable } from 'rxjs';
import { environment } from '@env/environment';
import { HttpOptions, ID_TYPE, IResponse } from '@model/response.model';
import { LoadingService } from './loading.service';
import { ERROR_CONTEXT } from '@constant/error.registry';

/**
 * A reusable API service for making HTTP requests to the backend.
 * - Automatically adds JSON headers to all requests.
 * - Appends base API URL from environment config.
 * - Shows and hides a global loading indicator via `LoadingService`.
 * - Provides common REST methods: GET, POST, PUT, DELETE, and batch delete.
 */
@Injectable({
  providedIn: 'root',
})
export class ApiService {
  readonly baseUrl = environment.host_api;
  readonly gisUrl = environment.gis_api;
  readonly auth = environment.auth;
  readonly notification = environment.notification;
  readonly sys = environment.sys;
  readonly exploration = environment.exploration;
  readonly mineclosure = environment.mineclosure;
  readonly resource = environment.resource;
  readonly original = environment.original;
  readonly geological = environment.geological;

  constructor(
    private readonly http: HttpClient,
    private readonly loadingService: LoadingService
  ) {}

  /**
   * Returns the default headers for API requests.
   * Currently only sets 'Content-Type' to 'application/json'.
   */
  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
    });
  }

  private getBaseUrl(apiHost?: string): string {
    switch (apiHost) {
      case 'gis':
        return this.gisUrl;
      case 'api':
        return this.baseUrl;

      default:
        // Default to the base URL if no specific host is provided
        return this.baseUrl;
    }
  }

  private getRouterUrl(routerHost?: string): string {
    switch (routerHost) {
      case 'auth':
        return '/' + this.auth;
      case 'notification':
        return '/' + this.notification;
      case 'sys':
        return '/' + this.sys;
      case 'exploration':
        return '/' + this.exploration;
      case 'mine-closure':
        return '/' + this.mineclosure;
      case 'resource':
        return '/' + this.resource;
      case 'original':
        return '/' + this.original;
      case 'geological':
        return '/' + this.geological;
      default:
        // Default to the base URL if no specific host is provided
        return '';
    }
  }

  /**
   * Makes a GET request to the specified endpoint.
   */

  get<T>(endpoint: string, options?: HttpOptions): Observable<IResponse<T>> {
    this.loadingService.show();
    return this.http
      .get<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        {
          headers: this.getHeaders(),
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }

  /**
   * Returns a specific resource by ID.
   */
  getById<T>(
    endpoint: string,
    id: ID_TYPE,
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();
    return this.http
      .get<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}/${id}`,
        {
          headers: this.getHeaders(),
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }

  /**
   * Creates a new resource.
   */
  post<T>(
    endpoint: string,
    data: unknown,
    queryParams?: Record<string, string | number | boolean>,
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();
    const params = queryParams
      ? new HttpParams({ fromObject: queryParams })
      : undefined;
    return this.http
      .post<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        data,
        {
          headers: this.getHeaders(),
          params,
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }

  /**
   * Updates an existing resource by ID.
   */
  put<T>(
    endpoint: string,
    data: unknown,
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();
    return this.http
      .put<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        data,
        {
          headers: this.getHeaders(),
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }

  /**
   * Deletes a resource by ID.
   */
  delete<T>(
    endpoint: string,
    queryParams: Record<string, string | number | boolean>,
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();
    const params = new HttpParams({ fromObject: queryParams });
    return this.http
      .delete<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        {
          headers: this.getHeaders(),
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          params,
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }

  /**
   * Deletes multiple resources by their IDs.
   * Sends the IDs in the request body for batch deletion.
   */
  deleteBatch<T>(
    endpoint: string,
    ids: ID_TYPE[],
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();
    const params = new HttpParams({ fromObject: { ids: ids } });
    return this.http
      .delete<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        {
          headers: this.getHeaders(),
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          params,
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }

  /**
   * Creates a new resource.
   */
  postFile<T>(
    endpoint: string,
    data: unknown,
    queryParams?: Record<string, string | number | boolean>,
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();
    const params = queryParams
      ? new HttpParams({ fromObject: queryParams })
      : undefined;
    return this.http
      .post<IResponse<T>>(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        data,
        {
          params,
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }
  putFile<T>(
    endpoint: string,
    data: FormData,
    queryParams?: Record<string, string | number | boolean>,
    options?: HttpOptions
  ): Observable<IResponse<T>> {
    this.loadingService.show();

    const { ...restOptions } = options || {};

    const params = queryParams
      ? new HttpParams({ fromObject: queryParams })
      : undefined;

    return this.http
      .put<IResponse<T>>(
        `${this.getBaseUrl(restOptions?.apiHost)}${this.getRouterUrl(restOptions?.routerHost)}/${endpoint}`,
        data,
        {
          params,
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: restOptions?.customError?.silent ?? false,
          }),
          ...restOptions,
        }
      )
      .pipe(finalize(() => this.loadingService.hide()));
  }

  /**
   * Downloads a file as blob from the specified endpoint.
   */
  download(endpoint: string, options?: HttpOptions): Observable<Blob> {
    this.loadingService.show();
    return this.http
      .get(
        `${this.getBaseUrl(options?.apiHost)}${this.getRouterUrl(options?.routerHost)}/${endpoint}`,
        {
          headers: this.getHeaders(),
          context: new HttpContext().set(ERROR_CONTEXT, {
            silent: options?.customError?.silent ?? false,
          }),
          responseType: 'blob',
          ...options,
        }
      )
      .pipe(
        finalize(() => this.loadingService.hide()) // stop loading on success or error
      );
  }
}
