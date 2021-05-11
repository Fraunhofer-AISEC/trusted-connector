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

    // If we receive 401 (or other error), log out the user immediately.
    return next.handle(request)
      .pipe(catchError((err: HttpErrorResponse) => throwError(err)));
  }
}
