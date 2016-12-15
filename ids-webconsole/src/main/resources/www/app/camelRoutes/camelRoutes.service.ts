import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

import { Route } from './route';

declare var API_URL: string;

@Injectable()
export class CamelRoutesService {
  constructor(private http: Http) { }

  getApps() {
    return this.http.get(API_URL + '/routes/list/')
               .map(response => {
                 return response.json() as Route[]
               })
  }
}
