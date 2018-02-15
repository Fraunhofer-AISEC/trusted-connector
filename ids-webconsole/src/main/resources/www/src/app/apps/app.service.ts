import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { App, DockerHubApp } from './app';
import { Cml } from './cml';
import { Result } from '../result';

import { environment } from '../../environments/environment';

@Injectable()
export class AppService {
  constructor(private http: HttpClient) {
    // console.log('constructor AppService');
  }

  getApps(): Observable<Array<App>> {
    return this.http.get<Array<App>>(environment.apiURL + '/apps/list/');
  }

  stopApp(appId: string): Observable<Result> {
    return this.http.get<Result>(environment.apiURL + '/apps/stop/' + encodeURIComponent(appId));
  }

  startApp(appId: string): Observable<Result> {
    return this.http.get<Result>(environment.apiURL + '/apps/start/' + encodeURIComponent(appId));
  }

  getCmlVersion(): Observable<Cml> {
    return this.http.get<Cml>(environment.apiURL + '/apps/cml_version');
  }

  searchApps(term: string): Observable<Array<DockerHubApp>> {
      var company : string = 'fhgaisec';
      var limit : number = 100; 
      var url: string = 'http://ids.aisec.fraunhofer.de/cors/https://hub.docker.com/v2/repositories/'+company+'/\?page_size\='+limit;
      console.log("Getting from url " + url);
      var result = this.http.get(url).map(resp => resp["results"] as Array<DockerHubApp>);
      return result;
    }

}
