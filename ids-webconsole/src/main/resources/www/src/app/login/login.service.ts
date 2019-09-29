import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, mergeMap, takeWhile } from 'rxjs/operators';

import { ApplicationHttpClient, HTTP_INJECTION_TOKEN } from '../application-http-client.service';

import { Token } from './token.interface';

@Injectable()
export class LoginService {

    constructor(@Inject(HTTP_INJECTION_TOKEN) private readonly http: ApplicationHttpClient) {
    }

    public login(username:string, password:string ): Observable<any>  {
        const headers = {
            headers: new HttpHeaders({
                contentType: 'application/json',
                responseType: 'application/json'
            })
        };
        return this.http.post<Token>('/user/login', {username, password}, headers).pipe(map(token => {
          if (token) {
            // login successful if there's a jwt token in the response
            // store user details and jwt token in local storage to keep user logged in between page refreshes
            localStorage.setItem('currentUser', token.token);
            return token.token;
            }
            return {};
          }));
    }

    public logout(): void  {
      localStorage.removeItem('currentUser');
    }
}
