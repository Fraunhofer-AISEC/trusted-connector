import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

import { App } from './app';

import { environment } from '../../environments/environment';

@Injectable()
export class AppService {
  constructor(private http: Http) {
    console.log("constructor AppService");
  }

  getApps() {
    return this.http.get(environment.apiURL + '/apps/list/')
               .map(response => {
                 return response.json() as App[];
               });
  }

  stopApp(appId: string) {
      return this.http.get(environment.apiURL + '/apps/stop?containerId=' + appId)
                 .map(response => {
                   return response.json() as string;
                 });
  }

  startApp(appId: string) {
      return this.http.get(environment.apiURL + '/apps/start?containerId=' + appId)
                 .map(response => {
                  return response.json() as string;
                 });
  }
}
