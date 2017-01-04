import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

import { App } from './app';

declare var API_URL: string;

@Injectable()
export class AppService {
  constructor(private http: Http) { }

  getApps() {
    return this.http.get(API_URL + '/apps/list/')
               .map(response => {
                 return response.json() as App[]
               })
  }
}
