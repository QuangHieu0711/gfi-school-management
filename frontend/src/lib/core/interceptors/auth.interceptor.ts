import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '@service';
import { AUTH_API_ENDPOINT } from '@model/auth.model';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly authService: AuthService) {}

  // Intercepts outgoing HTTP requests and attaches the access token (if available)
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const isAddressKitRequest = req.url.includes('/address-kit/') || req.url.includes('production.cas.so/address-kit/');
    const isHcmEsbRequest = req.url.includes('/hcmesb-test/') || req.url.includes('hcmesb.tphcm.gov.vn/');
    const isAuthRequest = [
      AUTH_API_ENDPOINT.AUTH_TOKEN,
      AUTH_API_ENDPOINT.REFRESH_TOKEN,
    ].some((endpoint) => req.url.includes(endpoint));

    if (isAuthRequest) {
      return next.handle(req.clone({ headers: req.headers.delete('Authorization') }));
    }

    if (isAddressKitRequest || isHcmEsbRequest) {
      return next.handle(req);
    }

    const TOKEN = this.authService.getAccessToken();

    // Clone the request and add the Authorization header
    // Pass the modified request to the next handler in the chain
    return next.handle(this.authService.addTokenHeader(req, TOKEN));
  }
}
