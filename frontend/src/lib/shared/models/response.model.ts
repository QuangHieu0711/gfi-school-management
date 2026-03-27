import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpParams,
  HttpRequest,
} from '@angular/common/http';
import { ToastService } from '@service';
import { Observer } from 'rxjs';

export interface IResponse<T> {
  timestamp: string;
  service: string;
  sessionId: string;
  requestId: string;
  code: number;
  message: string;
  data: T;
  [key: string]: unknown;
}

export interface ITableResponse<T> {
  pageNo: number;
  pageSize: number;
  totalCount: number;
  totalPage: number;
  data: T[];
  // New API structure
  items: T[];
  recordTotal: number;
  pageTotal?: number;
}

export type ID_TYPE = string | number;

export interface QueuedRequest<T> {
  req: HttpRequest<T>;
  next: HttpHandler;
  observer: Observer<HttpEvent<T>>;
}

export interface HttpOptions extends Record<string, unknown> {
  params?:
    | HttpParams
    | Record<
        string,
        string | number | boolean | readonly (string | number | boolean)[]
      >;
  customError?: ErrorHandlingContext;
  apiHost?: string; // Override the default API host
  routerHost?: string;
}

export interface ErrorHandlingContext {
  silent?: boolean; // Don't show any toast
}

export type ErrorRegistry = Record<
  string | number,
  Record<string | number, ErrorHandlerFn>
>;

export type ErrorHandlerFn = (
  toast: ToastService,
  t: (key: string, params?: Record<string, unknown>) => string,
  error: HttpErrorResponse
) => void;
