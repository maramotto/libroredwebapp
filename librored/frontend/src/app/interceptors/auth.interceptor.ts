import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(private authService: AuthService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const accessToken = this.authService.getAccessToken();

    // Add JWT token to requests if available
    let authReq = req;
    if (accessToken) {
      authReq = this.addTokenToRequest(req, accessToken);
    }

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // If 401 error and we have a refresh token, try to refresh
        if (error.status === 401 && accessToken && !this.isRefreshRequest(req)) {
          return this.handle401Error(authReq, next);
        }

        // For other errors, just pass them through
        return throwError(error);
      })
    );
  }

  private addTokenToRequest(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  private isRefreshRequest(request: HttpRequest<any>): boolean {
    return request.url.includes('/api/auth/refresh');
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((authResponse) => {
          this.isRefreshing = false;
          if (authResponse.status === 'SUCCESS' && authResponse.accessToken) {
            this.refreshTokenSubject.next(authResponse.accessToken);
            return next.handle(this.addTokenToRequest(request, authResponse.accessToken));
          } else {
            // Refresh failed, redirect to login
            this.authService.logOut();
            this.router.navigate(['/login']);
            return throwError('Token refresh failed');
          }
        }),
        catchError((error) => {
          this.isRefreshing = false;
          this.authService.logOut();
          this.router.navigate(['/login']);
          return throwError(error);
        })
      );
    } else {
      // Wait for the refresh to complete
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap(token => {
          return next.handle(this.addTokenToRequest(request, token));
        })
      );
    }
  }
}