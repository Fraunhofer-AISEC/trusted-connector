import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

/*
  Intercepts outgoing requests and adds an HTTP "Authorization" header, if a bearer token is present in local storage.
*/
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

    public intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Add Authorization header with jwt token to outgoing HTTP request
        const currentUser = sessionStorage.getItem('currentUser');
        if (currentUser) {
            request = request.clone({
                setHeaders: {
                    Authorization: `Bearer ${currentUser}`
                }
            });
        }

        return next.handle(request).pipe(
            catchError((err: HttpErrorResponse) => {
                // If we receive 401 (Unauthorized), trigger logout to redirect user to login form
                if (err.status === 401) {
                    sessionStorage.removeItem('currentUser');
                }
                return throwError(err);
            }));
    }
}
