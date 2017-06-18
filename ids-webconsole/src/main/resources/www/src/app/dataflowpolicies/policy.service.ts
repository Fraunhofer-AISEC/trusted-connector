import { Injectable } from '@angular/core';
import { Headers, Http, Response, RequestOptions } from '@angular/http';
import {Observable} from 'rxjs/Rx';

import 'rxjs/add/operator/map';

import { Policy } from './policy';

import { environment } from '../../environments/environment';

@Injectable()
export class PolicyService {
  constructor(private http: Http) { }

  getPolicies() {
    return this.http.get(environment.apiURL + '/policies/list')
               .map(response => {
                 return response.json() as string[];
               });
  }

    install(model: FormData) {
        let headers = new Headers({ 'Content-Type': 'multipart/form-data' });
        let options = new RequestOptions({ headers: headers });
        
        let result = this.http.post(environment.apiURL + '/policies/install', model, options)
                         .catch((error:any) => Observable.throw(error || 'Server error')) //...errors if
                         .subscribe();
        return result;
    } 

    private handleError (error: Response | any) {
      // In a real world app, we might use a remote logging infrastructure
      let errMsg: string;
      if (error instanceof Response) {
        const body = error.json() || '';
        const err = body.error || JSON.stringify(body);
        errMsg = `${error.status} - ${error.statusText || ''} ${err}`;
      } else {
        errMsg = error.message ? error.message : error.toString();
      }
      console.error(errMsg);
      return Promise.reject(errMsg);
    }

}
