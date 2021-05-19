import { HttpRequest, HttpHandler, HttpErrorResponse, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { LoginService } from '../login/login.service';

/*
  Intercepts outgoing requests and adds an HTTP "Authorization" header, if a bearer token is present in local storage.
*/
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(public loginService: LoginService) { }

  public intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // add Authorization header with jwt token to outgoing HTTP request
    const currentUser = localStorage.getItem('currentUser');
    if (currentUser) {
      request = request.clone({
        setHeaders: {
          /* eslint-disable @typescript-eslint/naming-convention */
          Authorization: `Bearer ${currentUser}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((err: HttpErrorResponse) => {
        // If we receive 401 (Unauthorized), trigger logout to redirect user to login form
        if (err.status === 401) {
          this.loginService.logout();
        }
        return throwError(err);
      }));
  }
}
