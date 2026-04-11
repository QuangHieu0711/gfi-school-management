import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse,
} from '@angular/common/http';
import {
  Observable,
  BehaviorSubject,
  throwError,
  Subject,
  Observer,
} from 'rxjs';
import {
  retry,
  catchError,
  filter,
  switchMap,
  take,
  concatMap,
  tap,
  finalize,
} from 'rxjs/operators';
import { AuthService, LanguageService, ToastService } from '@service';
import { AUTH_API_ENDPOINT } from '@model/auth.model';
import { ErrorHandlingContext, QueuedRequest } from '@model/response.model';
import { ERROR_CONTEXT, ERROR_REGISTRY } from '@constant/error.registry';

/**
 * An HTTP interceptor that handles API errors globally.
 * - Automatically retries GET requests on server errors.
 * - Queues requests for token refresh if unauthorized.
 * - Supports custom error handling based on status codes.
 */
@Injectable()
export class ApiErrorInterceptor implements HttpInterceptor {
  private readonly MAX_RETRY = 3;
  private readonly RETRY_DELAY_MS = 1000;
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);
  private readonly requestQueue = new Subject<QueuedRequest<unknown>>();

  constructor(
    private readonly authService: AuthService,
    private readonly toast: ToastService,
    private readonly languageService: LanguageService
  ) {
    this.processQueue();
  }

  /**
   * Intercepts all outgoing HTTP requests and handles errors globally
   * @param req The outgoing HTTP request.
   * @param next The next handler in the chain.
   * @returns An observable of the HTTP event.
   */
  intercept<T>(
    req: HttpRequest<T>,
    next: HttpHandler
  ): Observable<HttpEvent<T>> {
    const context: ErrorHandlingContext = req.context.get(ERROR_CONTEXT);
    return next.handle(req).pipe(
      // Retry failed GET requests (server errors) up to MAX_RETRY times with delay
      retry({
        count: this.MAX_RETRY,
        delay: (error: HttpErrorResponse) => {
          // Retry after delay if it's a server error
          // if (error.status >= 500 && error.status < 600 && req.method === 'GET') return timer(this.RETRY_DELAY_MS);
          // Do not retry for non-5xx errors
          return throwError(() => error);
        },
      }),
      // Handle other errors
      catchError((error: HttpErrorResponse) => {
        // Skip intercepting i18n JSON file requests
        if (req.url.includes('assets/i18n/')) return throwError(() => error);

        const isAuthRequest = [
          AUTH_API_ENDPOINT.AUTH_TOKEN,
          AUTH_API_ENDPOINT.REFRESH_TOKEN,
        ].some((url) => req.url.includes(url));

        // If unauthorized and not a login request, queue the request for token refresh
        if (error.status === 401 && !isAuthRequest)
          return new Observable<HttpEvent<T>>((observer) =>
            this.requestQueue.next({
              req,
              next,
              observer,
            } as QueuedRequest<unknown>)
          );

        // Backend currently returns 403 when the access token is expired.
        // In that case, clear the local session and redirect to login.
        if (
          error.status === 403 &&
          !isAuthRequest &&
          this.authService.isAuthenticated()
        ) {
          this.authService.handleLogout();
        }

        // Otherwise, show appropriate error message
        return this.handleErrorRequest(error, context);
      })
    );
  }

  /**
   * Handles the error response by showing a toast notification based on the error status.
   * @param error The HTTP error response.
   * @returns An observable that throws the error.
   */
  private handleErrorRequest(
    error: HttpErrorResponse,
    context?: ErrorHandlingContext
  ): Observable<never> {
    console.error('Error:', error);
    if (context?.silent) return throwError(() => error);

    // Try to find handler by exact status + code first
    // Default to 'UNKNOWN' if error body is null or doesn't contain a code
    const code = error?.error?.code ?? 'UNKNOWN';
    const status = error.status;
    const t = this.languageService.instant.bind(this.languageService);

    const handler =
      ERROR_REGISTRY[status]?.[code] ??
      ERROR_REGISTRY[status]?.['DEFAULT'] ??
      ERROR_REGISTRY['DEFAULT']?.['DEFAULT'];

    if (handler) handler(this.toast, t, error);
    else
      console.warn(
        `No error handler found for status ${status} and code ${code}`,
        error
      );

    return throwError(() => error);
  }

  /**
   * Processes the request queue to handle queued requests.
   * This method is called when a request is queued for token refresh.
   */
  private processQueue(): void {
    this.requestQueue
      .pipe(concatMap((queued) => this.handleQueuedRequest(queued)))
      .subscribe();
  }

  /**
   * Handles a queued request by refreshing the token if needed, and retrying the request.
   * @param queued The queued request containing the original request, next handler, and observer.
   * @returns An observable of the HTTP event.
   */
  private handleQueuedRequest<T>(
    queued: QueuedRequest<T>
  ): Observable<HttpEvent<T>> {
    const { req, next, observer } = queued;

    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      // Attempt to refresh the token
      return this.authService.refreshToken().pipe(
        switchMap(() => {
          this.isRefreshing = false;
          const token = this.authService.getAccessToken();
          this.refreshTokenSubject.next(token);
          return this.retryRequest(req, next, observer, token);
        }),
        catchError((err) => {
          // If refresh fails, clear tokens and propagate error
          this.isRefreshing = false;
          this.authService.handleLogout();
          this.refreshTokenSubject.error(err);
          this.refreshTokenSubject = new BehaviorSubject<string | null>(null);
          observer.error(err);
          return throwError(() => err);
        })
      );
    } else {
      // If refresh is already in progress, wait for the new token
      return this.refreshTokenSubject.pipe(
        filter((token): token is string => token !== null),
        take(1),
        switchMap((token) => this.retryRequest(req, next, observer, token))
      );
    }
  }

  /**
   * Retries a previously failed request using a new token and completes the original observer.
   * @param req The original HTTP request.
   * @param next The next handler in the chain.
   * @param observer The original observer to complete.
   * @param token The new access token.
   * @returns An observable of the HTTP event.
   */
  private retryRequest<T>(
    req: HttpRequest<T>,
    next: HttpHandler,
    observer: Observer<HttpEvent<T>>,
    token: string
  ): Observable<HttpEvent<T>> {
    return next.handle(this.authService.addTokenHeader(req, token)).pipe(
      tap((event) => observer.next(event)),
      catchError((err) => {
        observer.error(err);
        return throwError(() => err);
      }),
      finalize(() => observer.complete())
    );
  }
}
