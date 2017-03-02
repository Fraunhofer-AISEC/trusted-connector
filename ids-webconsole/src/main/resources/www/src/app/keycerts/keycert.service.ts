import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';

import 'rxjs/add/operator/map';

import { Certificate } from './certificate';

import { environment } from '../../environments/environment';

@Injectable()
export class CertificateService {

  constructor(private http: Http) { }

  getIdentities() {
    return this.http.get(environment.apiURL + '/certs/list_identities')
               .map(response => {
                 return response.json() as Certificate[];
               });
  }

  getCertificates() {
    return this.http.get(environment.apiURL + '/certs/list_certs')
               .map(response => {
                 return response.json() as Certificate[];
               });
  }

  // TODO Create identity

  deleteEntry(alias: string, file: string) {
    let params = new URLSearchParams();
    params.set('alias', alias);
    params.set('file', file)

    return this.http.get(environment.apiURL + '/certs/delete/', { search: params })
               .map(response => {
                 return response.json() as string;
               });
  }
}
