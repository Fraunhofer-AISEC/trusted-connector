import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { EstEnrollment } from './est-enrollment.interface';
import { EstReEnrollment } from './est-re-enrollment.interface';

@Injectable()
export class ESTService {

    constructor(private readonly http: HttpClient) {
    }

    // Request EST root certificate
    public requestEstCaCert(url: string, hash: string): Observable<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});

        return this.http.post(environment.apiURL + '/certs/est_ca_certs', {url, hash}, {
            headers,
            responseType: 'text'
        });
    }

    // Save root certificate to connector
    public uploadEstCaCert(cert: string): Observable<string> {
        const headers = new HttpHeaders({'Content-Type': 'text/plain'});

        return this.http.post(environment.apiURL + '/certs/store_est_ca_cert', cert, {
            headers,
            responseType: 'text'
        });
    }

    // Create new identity via EST
    public createIdentity(data: EstEnrollment): Observable<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});

        return this.http.post(environment.apiURL + '/certs/request_est_identity', data, {
            headers,
            responseType: 'text'
        });
    }

    // Renew an existing identity identified by its alias via the EST
    public renewIdentity(data: EstReEnrollment) {
        return this.http.post(environment.apiURL + '/certs/renew_est_identity', data, {
            headers: new HttpHeaders({'Content-Type': 'application/json'}),
            responseType: 'text'
        });
    }
}
