import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Certificate } from './certificate';
import { Identity } from './identity.interface';

@Injectable()
export class CertificateService {

  constructor(private readonly http: HttpClient) { }

  public getIdentities(): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(environment.apiURL + '/certs/list_identities');
  }

  public getCertificates(): Observable<Certificate[]> {
    return this.http.get<Certificate[]>(environment.apiURL + '/certs/list_certs');
  }

  public createIdentity(identity: Identity): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = JSON.stringify(identity);

    return this.http.post(environment.apiURL + '/certs/create_identity', body, {
      headers,
      responseType: 'text'
    });
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

  public uploadCert(inFile: File): Observable<string> {
    const formData: any = new FormData();
    formData.append('upfile', inFile, inFile.name);

    return this.http.post(environment.apiURL + '/certs/install_trusted_cert', formData, { responseType: 'text' });
  }
}
