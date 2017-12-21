import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import {HttpClient, HttpHeaders} from '@angular/common/http';

import 'rxjs/add/operator/map';

import { Route } from './route';
import { RouteMetrics } from './route-metrics';
import { ValidationInfo } from './validation';

import { environment } from '../../environments/environment';

@Injectable()
export class RouteService {
  constructor(private http: Http, private httpClient: HttpClient) { }
  getRoute(routeId: string) {
    return this.http.get(environment.apiURL + '/routes/get/' + routeId)
               .map(response => {
                 return response.json() as Route;
               });
  }

  getValidationInfo(routeId: string) {
    return this.http.get(environment.apiURL + "/routes/validate/" + routeId)
               .map(response => {
                 return response.json() as ValidationInfo;
               });
  }

  getMetrics() {
    return this.http.get(environment.apiURL + '/routes/metrics')
               .map(response => {
                 return response.json() as RouteMetrics;
               });
  }

  getRoutes() {
    return this.http.get(environment.apiURL + '/routes/list/')
               .map(response => {
                 return response.json() as Route[];
               });
  }

  stopRoute(routeId: string) {
      return this.http.get(environment.apiURL + '/routes/stoproute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }

  startRoute(routeId: string) {
    // -------------------------------------------------------------------------------
    // Hardcoded just for demonstration
    if(routeId == "OPC-UA: Read Engine Power (Trusted)") {
      this.http.post('http://' + window.location.hostname + ':8282/led/1/power/true', {}).subscribe();
    } else if(routeId == "OPC-UA: Read Engine Power (Untrusted)") {
      this.http.post('http://' + window.location.hostname + ':8282/led/2/power/true', {}).subscribe();
    } else if(routeId == "IDS-Protocol: Transmit Connector Data") {
      this.http.post('http://' + window.location.hostname + ':8282/led/3/power/true', {}).subscribe();
    } else if(routeId == "HTTPS: Transmit Cloud Data") {
      this.http.post('http://' + window.location.hostname + ':8282/led/4/power/true', {}).subscribe();
    }
    // -------------------------------------------------------------------------------

      //Start Camel route
      return this.http.get(environment.apiURL + '/routes/startroute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }

  save(route: Route)  {
      //Start Camel route
            const headers = new HttpHeaders().set('Content-Type', 'application/json; charset=utf-8');
            return this.httpClient.post(environment.apiURL + '/routes/save/', route,{headers: headers});
  }

  listEndpoints() {
      return this.http.get(environment.apiURL + '/routes/list_endpoints')
                 .map(response => {
                   return response.json() as string[];
                 });
  }

  listComponents() {
      return this.http.get(environment.apiURL + '/routes/list_components')
                 .map(response => {
                   return response.json() as string[];
                 });
  }
}
