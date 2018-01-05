import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';

import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';

import {Result} from '../result';
import {Route} from './route';
import {RouteMetrics} from './route-metrics';
import {ValidationInfo} from './validation';

import { environment} from '../../environments/environment';

@Injectable()
export class RouteService {
  constructor(private httpClient: HttpClient) {}

  getRoute(routeId: string): Observable<Route> {
    return this.httpClient.get(environment.apiURL + '/routes/get/' + routeId) as Observable<Route>;
  }

  getValidationInfo(routeId: string): Observable<ValidationInfo> {
    return this.httpClient.get(environment.apiURL + '/routes/validate/' + routeId) as Observable<ValidationInfo>;
  }

  getMetrics(): Observable<RouteMetrics> {
    return this.httpClient.get(environment.apiURL + '/routes/metrics') as Observable<RouteMetrics>;
  }

  getRoutes(): Observable<Route[]> {
    return this.httpClient.get(environment.apiURL + '/routes/list/') as Observable<Route[]>;
  }

  stopRoute(routeId: string): Observable<Result> {
     // Stop Camel route
    return this.httpClient.get(environment.apiURL + '/routes/stoproute/' + routeId) as Observable<Result>;
  }

  startRoute(routeId: string): Observable<Result> {
    // Start Camel route
    return this.httpClient.get(environment.apiURL + '/routes/startroute/' + routeId) as Observable<Result>;
  }

  save(route: Route): Observable<Result> {
    // Save Camel route
    const headers = new HttpHeaders().set('Content-Type', 'application/json; charset=utf-8');
    console.log('Sending ' + route.txtRepresentation);
    return this.httpClient.post(environment.apiURL + '/routes/save', JSON.stringify(route), {headers: headers}) as Observable<Result>;
  }

  listEndpoints(): Observable<string[]> {
    return this.httpClient.get(environment.apiURL + '/routes/list_endpoints') as Observable<string[]>;
  }

  listComponents(): Observable<string[]> {
    return this.httpClient.get(environment.apiURL + '/routes/list_components') as Observable<string[]>;
  }
}