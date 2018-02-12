import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {RequestOptions, Request, RequestMethod} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';

import { Certificate } from './certificate';
import { Identity } from './identity.interface';

import { environment } from '../../environments/environment';

@Injectable()
export class CertificateService {

  constructor(private http: Http) { }

  getIdentities(): Observable<Certificate[]> {
    return this.http.get(environment.apiURL + '/certs/list_identities').map(response => { return response.json() as Certificate[]; });
  }

  getCertificates(): Observable<Certificate[]> {
    return this.http.get(environment.apiURL + '/certs/list_certs').map(response => { return response.json() as Certificate[]; });
  }

  createIdentity(identity: Identity): Observable<string> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    let body = JSON.stringify(identity);
    return this.http.post(environment.apiURL + '/certs/create_identity', body, options )
      .map((res: Response) => {return res.json() as string; });
  }

  deleteCert(alias: string): Observable<string> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post(environment.apiURL + '/certs/delete_cert', alias, options )
      .map((res: Response) => {return res.json() as string; });
  }

  deleteIdentity(alias: string) {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post(environment.apiURL + '/certs/delete_identity', alias, options )
      .map((res: Response) => {return res.json() as string; });
  }

  uploadCert(inFile: File) {
        return new Promise((resolve, reject) => {
           let formData: any = new FormData();
           let xhr = new XMLHttpRequest();
           formData.append('upfile', inFile, inFile.name);

           xhr.onreadystatechange = function () {
               if (xhr.readyState === 4) {
                   if (xhr.status === 200) {
                       console.log(xhr.response);
                   } else {
                       console.log(xhr.response);
                   }
               }
           };
           xhr.open('POST', environment.apiURL + '/certs/install_trusted_cert', true);
           xhr.send(formData);
       });
  }
}
