import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Identity } from './identity.interface';

@Injectable()
export class ESTService {

  constructor(private readonly http: HttpClient) { }

// EST Root cert
  // request root  certificate from est
    public requestEstCaCert(url: string, hash: string): Observable<string> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        const body = JSON.stringify([url, hash]);
        return this.http.post(environment.apiURL + '/certs/est_ca_cert', body, {
          headers,
          responseType: 'text'
        });
      }

  // save root certificate to connector
  public uploadCert(cert: string): Observable<string> {
  const body = cert;
      return this.http.post(environment.apiURL + '/certs/install_trusted_cert', body, { responseType: 'text' });
    }

// Client certs
  // create new identity via est
  public createIdentity(identity: Identity, username: string, password: string, esturl: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = JSON.stringify([identity,username,password,esturl]);
    console.log(username+password+esturl);
    return this.http.post(environment.apiURL + '/certs/request_est_identity', body, {
      headers,
      responseType: 'text'
    });
  }
}
