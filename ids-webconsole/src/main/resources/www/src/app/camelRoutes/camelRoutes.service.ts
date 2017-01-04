import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

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

  stopRoute(routeId: string) {
      return this.http.get(API_URL + '/routes/stoproute/' + routeId)
                 .map(response => {
                   return response.json() as string
                 })
  }

  startRoute(routeId: string) {
      return this.http.get(API_URL + '/routes/startroute/' + routeId)
                 .map(response => {
                   return response.json() as string
                 })
  }
}
