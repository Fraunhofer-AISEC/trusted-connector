import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/toPromise';

import { Route } from './route';

declare var API_URL: string;

@Injectable()
export class CamelRoutesService {
  constructor(private http: Http) { }

  getRoutes() {
  return this.http.get(API_URL + '/routes/list/')
              .map(response => {
                 return response.json() as Route[]
               })
  }

  /*getRoutes(): Promise<Route[]> {
    return this.http.get('http://localhost:8181/cxf/api/v1/routes/list')
               .toPromise()
               .then(response => response.json().data as Route[])
               .catch(this.handleError);
  }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error); // for demo purposes only
    return Promise.reject(error.message || error);
  }*/
}
