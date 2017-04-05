import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {RequestOptions, Request, RequestMethod} from '@angular/http';
import {Observable} from 'rxjs/Observable';
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

  uploadCert(inFile: File) {
  /*  let formData:FormData = new FormData();
        formData.append('degree_attachment', inFile, inFile.name);
        let headers = new Headers();
        headers.append('Accept', 'application/json');
        let options = new RequestOptions({ headers: headers });
        this.http.post(environment.apiURL + '/certs/upload', formData,options)
            .map(response => {
              return response.json() as string;
            })
            .subscribe(
                data => console.log('success'),
                error => console.log(error)
            )*/
        return new Promise((resolve, reject) => {
           var formData: any = new FormData();
           var xhr = new XMLHttpRequest();
           formData.append("uploads", inFile, inFile.name);

           xhr.onreadystatechange = function () {
               if (xhr.readyState == 4) {
                   if (xhr.status == 200) {
                       console.log(xhr.response);
                   } else {
                       console.log(xhr.response);
                   }
               }
           }
           xhr.open("POST", environment.apiURL + '/certs/upload', true);
           xhr.send(formData);
       });
  }


}
