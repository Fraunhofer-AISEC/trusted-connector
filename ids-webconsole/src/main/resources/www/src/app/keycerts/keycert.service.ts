import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

import { Certificate } from './certificate';

import { environment } from '../../environments/environment';

@Injectable()
export class CertificateService {

  constructor(private http: Http) { }

  getCertificates() {
    return this.http.get(environment.apiURL + '/certs/list/')
               .map(response => {
                 return response.json() as Certificate[];
               });
  }
}
