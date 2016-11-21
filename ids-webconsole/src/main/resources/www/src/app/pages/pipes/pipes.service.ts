import {Injectable} from '@angular/core';
import { Component, CORE_DIRECTIVES } from 'angular2/angular2';
import { Task } from '../datatypes/task';
import { Http, Response, Headers, RequestOptions, JsonpModule, Jsonp } from '@angular/http';
import {Observable} from 'rxjs/Rx';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

@Injectable()
export class PipesService {

  constructor(public http: Http, public jsonp: Jsonp) {
    console.log('PipesService created.', http);
  }
   
  
  /**
   * Retrieve raw routes defintion from backend (e.g. a Camel routes config)
   */
  getRoutesRaw() : Observable<any> {
    // return an observable    
    return this.jsonp.get('http://localhost:8181/cxf/api/v1/routes/get_raw?_jsonp=JSONP_CALLBACK')
    .map(res => {
                return res.json();
            });
  }
}
