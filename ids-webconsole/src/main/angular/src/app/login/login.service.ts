import { HttpHeaders } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApplicationHttpClient, HTTP_INJECTION_TOKEN } from '../application-http-client.service';

import { ActivatedRoute, Router } from '@angular/router';

@Injectable()
export class LoginService {

    constructor(@Inject(HTTP_INJECTION_TOKEN) private readonly http: ApplicationHttpClient,
                private route: ActivatedRoute,
                private router: Router) {
    }

    public login(username: string, password: string): Observable<string> {
        return this.http.post('/user/login', {username, password}, {
            headers: new HttpHeaders({ contentType: 'application/json' }),
            responseType: 'text'
        }).pipe(map(token => {
            if (token) {
                // login successful if there's a jwt token in the response
                // store user details and jwt token in local storage to keep user logged in between page refreshes
                sessionStorage.setItem('currentUser', token);
                return token;
            } else {
                throw new Error('No token received, login credentials invalid?');
            }
        }));
    }

    public logout(): void {
        sessionStorage.removeItem('currentUser');
        this.router.navigate(['/login']);
    }
}
