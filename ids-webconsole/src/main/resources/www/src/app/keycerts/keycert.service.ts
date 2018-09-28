import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { Certificate } from './certificate';
import { Identity } from './identity.interface';

@Injectable()
export class CertificateService {

  constructor(private http: HttpClient) { }

  getIdentities(): Observable<Array<Certificate>> {
    return this.http.get<Array<Certificate>>(environment.apiURL + '/certs/list_identities');
  }

  getCertificates(): Observable<Array<Certificate>> {
    return this.http.get<Array<Certificate>>(environment.apiURL + '/certs/list_certs');
  }

  createIdentity(identity: Identity): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = JSON.stringify(identity);

    return this.http.post(environment.apiURL + '/certs/create_identity', body, {
      headers,
      responseType: 'text'
    });
  }

  deleteCert(alias: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    return this.http.post(environment.apiURL + '/certs/delete_cert', alias, {
      headers,
      responseType: 'text'
    });
  }

  deleteIdentity(alias: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    return this.http.post(environment.apiURL + '/certs/delete_identity', alias, {
      headers,
      responseType: 'text'
    });
  }

  uploadCert(inFile: File): Observable<string> {
    const formData: any = new FormData();
    formData.append('upfile', inFile, inFile.name);

    return this.http.post(environment.apiURL + '/certs/install_trusted_cert', formData, { responseType: 'text' });
  }
}
