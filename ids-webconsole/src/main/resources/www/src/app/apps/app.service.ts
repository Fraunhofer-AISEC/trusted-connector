import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import 'rxjs/add/operator/map';

import { App } from './app';
import { Cml } from './cml';
import { Result } from '../result';

import { environment } from '../../environments/environment';

@Injectable()
export class AppService {
  constructor(private http: HttpClient) {
    console.log('constructor AppService');
  }

  getApps(): Observable<App[]> {
    return this.http.get(environment.apiURL + '/apps/list/') as Observable<App[]>;
  }

  stopApp(appId: string): Observable<Result> {
    return this.http.get(environment.apiURL + '/apps/stop?containerId=' + appId) as Observable<Result>;
  }

  startApp(appId: string): Observable<Result> {
    return this.http.get(environment.apiURL + '/apps/start?containerId=' + appId) as Observable<Result>;
  }

  getCmlVersion(): Observable<Cml> {
    return this.http.get(environment.apiURL + '/apps/cml_version') as Observable<Cml>;
  }

}
