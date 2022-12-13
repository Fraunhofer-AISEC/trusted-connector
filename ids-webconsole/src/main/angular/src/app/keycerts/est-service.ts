import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { EstEnrollment } from './est-enrollment.interface';

@Injectable()
export class ESTService {

    constructor(private readonly http: HttpClient) {
    }

    // Request EST root certificate
    public requestEstCaCert(url: string, hash: string): Observable<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});
        const body = JSON.stringify({url, hash});
        return this.http.post(environment.apiURL + '/certs/est_ca_cert', body, {
            headers,
            responseType: 'text'
        });
    }

    // Save root certificate to connector
    public uploadCert(cert: string): Observable<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});
        const body = JSON.stringify(cert);
        return this.http.post(environment.apiURL + '/certs/store_est_ca_cert', body, {
            headers,
            responseType: 'text'
        });
    }

    // Create new identity via EST
    public createIdentity(data: EstEnrollment): Observable<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});
        const body = JSON.stringify(data);
        return this.http.post(environment.apiURL + '/certs/request_est_identity', body, {
            headers,
            responseType: 'text'
        });
    }
}
