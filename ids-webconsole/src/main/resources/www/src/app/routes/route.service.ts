import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

import { Route } from './route';

import { environment } from '../../environments/environment';

@Injectable()
export class RouteService {
  constructor(private http: Http) { }

  getRoute(route: any) {
    return this.http.get(environment.apiURL + '/routes/get/' + route)
               .map(response => {
                 return response.json() as Route;
               });
  }

  getRoutes() {
    return this.http.get(environment.apiURL + '/routes/list/')
               .map(response => {
                 return response.json() as Route[];
               });
  }

  stopRoute(routeId: string) {
      // -------------------------------------------------------------------------------
      // Hardcoded just for demonstration: Stop "trusted" scenario if magic route is off
      if (routeId=="Trusted_Sensors") {
        this.http.get('http://localhost:8080/sensordataapp/setScenario?id=0')
               .map(response => {
                 return response.json() as Route;
               });
      }
      // -------------------------------------------------------------------------------
      
      return this.http.get(environment.apiURL + '/routes/stoproute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }

  startRoute(routeId: string) {
      // -------------------------------------------------------------------------------
      // Hardcoded just for demonstration: Start "trusted" scenario if magic route is on
      if (routeId=="Trusted_Sensors") {
        this.http.get('http://localhost:8080/sensordataapp/setScenario?id=2')
               .map(response => {
                 return response.json() as Route;
               });
      }
      // -------------------------------------------------------------------------------
    
      //Start Camel route
      return this.http.get(environment.apiURL + '/routes/startroute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }
}
