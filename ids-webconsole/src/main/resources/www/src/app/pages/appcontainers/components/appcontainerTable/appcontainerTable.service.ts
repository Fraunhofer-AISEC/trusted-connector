import {Injectable} from '@angular/core';
import { Component, CORE_DIRECTIVES } from 'angular2/angular2';
import { Task } from '../datatypes/task';
import { Http, Response, Headers, RequestOptions, JsonpModule, Jsonp } from '@angular/http';
import {Observable} from 'rxjs/Rx';
import { AppContainer } from '../../appcontainers.component';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class AppContainerService {

  constructor(public http: Http, public jsonp: Jsonp) {
    console.log('AppContainer Service created.', http);
  }
   
  /**
   * Promise for retrieving list of application containers from REST backend (could be removed)
   */
  getData(): Promise<any> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        resolve(this.getAppList());
      }, 20);
    });
  }
  
  /**
   * Retrieve list of application containers from REST backend
   */
  getAppList() : Promise<any> {
    // return an observable    
    return this.jsonp.get('http://localhost:8181/cxf/api/apps?_jsonp=JSONP_CALLBACK')
    .toPromise()
    .then( (responseData:Response) => 
      responseData.json()
    );
  }
}
