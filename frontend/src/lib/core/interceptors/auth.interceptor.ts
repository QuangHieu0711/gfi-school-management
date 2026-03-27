import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '@service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly authService: AuthService) {}

  // Intercepts outgoing HTTP requests and attaches the access token (if available)
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const TOKEN = this.authService.getAccessToken();

    // Clone the request and add the Authorization header
    // Pass the modified request to the next handler in the chain
    return next.handle(this.authService.addTokenHeader(req, TOKEN));
  }
}
