import { Injectable } from '@angular/core';
import { Headers, Http, Response, RequestOptions } from '@angular/http';
import {Observable} from 'rxjs/Observable';

import { Settings } from './settings.interface';
import { environment } from '../../environments/environment';

@Injectable()
export class SettingsService {  
  constructor(private http: Http) { }
  
  getSettings() {
    return this.http.get(environment.apiURL + '/config/list/')
               .map(response => {
                 return response.json() as Settings;
               });
  }
  
  store(model: Settings): Observable<Response> {
    	let headers = new Headers({ 'Content-Type': 'application/json' });
    	let options = new RequestOptions({ headers: headers });
  
        let result = this.http.post(environment.apiURL + '/config/set', model, options);
        return result;
	}
  
}
