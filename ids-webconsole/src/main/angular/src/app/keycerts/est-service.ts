import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Certificate } from './certificate';
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
  public createIdentity(identity: Identity, username: string, password: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = JSON.stringify([identity,username,password]);
    console.log(username+password);
    return this.http.post(environment.apiURL + '/certs/create_identity', body, {
      headers,
      responseType: 'text'
    });
  }

  public getIdentities(): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(environment.apiURL + '/certs/list_identities');
  }

  public getCertificates(): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(environment.apiURL + '/certs/list_certs');
  }

  public deleteCert(alias: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    return this.http.post(environment.apiURL + '/certs/delete_cert', alias, {
      headers,
      responseType: 'text'
    });
  }

  public deleteIdentity(alias: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    return this.http.post(environment.apiURL + '/certs/delete_identity', alias, {
      headers,
      responseType: 'text'
    });
  }
}
