import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class StripFetchOptionsInterceptor implements HttpInterceptor {
  // Removes fetch-only properties from outgoing HTTP requests to avoid compatibility issues
  intercept<T>(req: HttpRequest<T>, next: HttpHandler): Observable<HttpEvent<T>> {
    const cleanedReq = this.stripFetchOnlyOptions(req);
    return next.handle(cleanedReq);
  }

  // Manually strips fetch-only properties that Angular’s HttpClient doesn’t recognize
  private stripFetchOnlyOptions<T>(req: HttpRequest<T>): HttpRequest<T> {
    const cloned = req.clone();

    // Step 1: Cast the immutable HttpRequest to a mutable object
    const mutable = cloned as unknown as Record<string, unknown>;

    // Step 2: Remove known fetch-only properties
    const fetchOnlyProps: string[] = ['cache', 'credentials', 'keepalive', 'mode', 'priority', 'redirect'] as const;

    for (const prop of fetchOnlyProps) {
      if (prop in mutable) {
        try {
          delete mutable[prop];
        } catch {
          // Fallback: overwrite the property with undefined if deletion fails
          Object.defineProperty(mutable, prop, {
            value: undefined,
            writable: true,
            configurable: true,
            enumerable: false,
          });
        }
      }
    }

    // Step 3: Cast back to HttpRequest<T> for safe downstream use
    return mutable as unknown as HttpRequest<T>;
  }
}
