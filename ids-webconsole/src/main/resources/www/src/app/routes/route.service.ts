import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Result, RouteResult } from '../result';

import { Route, RouteComponent } from './route';
import { RouteMetrics } from './route-metrics';
import { ValidationInfo } from './validation';

@Injectable()
export class RouteService {
  constructor(private readonly httpClient: HttpClient) { }

  getRoute(routeId: string): Observable<Route> {
    return this.httpClient.get(environment.apiURL + '/routes/get/' + routeId) as Observable<Route>;
  }

  getRouteAsString(routeId: string): Observable<string> {
    return this.httpClient.get(environment.apiURL + '/routes/getAsString/' + routeId, { responseType: 'text' });
  }

  getValidationInfo(routeId: string): Observable<ValidationInfo> {
    return this.httpClient.get(environment.apiURL + '/routes/validate/' + routeId) as Observable<ValidationInfo>;
  }

  getMetrics(): Observable<RouteMetrics> {
    return this.httpClient.get(environment.apiURL + '/routes/metrics') as Observable<RouteMetrics>;
  }

  getRoutes(): Observable<Array<Route>> {
    return this.httpClient.get(environment.apiURL + '/routes/list/') as Observable<Array<Route>>;
  }

  stopRoute(routeId: string): Observable<Result> {
    // Stop Camel route
    return this.httpClient.get(environment.apiURL + '/routes/stoproute/' + routeId) as Observable<Result>;
  }

  startRoute(routeId: string): Observable<Result> {
    // Start Camel route
    return this.httpClient.get(environment.apiURL + '/routes/startroute/' + routeId) as Observable<Result>;
  }

  saveRoute(routeId: string, routeString: string): Observable<RouteResult> {
    // Update Camel route
    const headers = new HttpHeaders().set('Content-Type', 'text/plain; charset=utf-8');
    // console.log('Sending Update: ' + routeString);

    return this.httpClient.post(environment.apiURL + '/routes/save/' + routeId,
      routeString, { headers }) as Observable<RouteResult>;
  }

  addRoute(routeString: string): Observable<Result> {
    // Save new Camel route
    const headers = new HttpHeaders().set('Content-Type', 'text/plain; charset=utf-8');
    // console.log('Sending New: ' + routeString);

    return this.httpClient.put(environment.apiURL + '/routes/add',
      routeString, { headers }) as Observable<Result>;
  }

  listComponents(): Observable<Array<RouteComponent>> {
    return this.httpClient.get<Array<RouteComponent>>(environment.apiURL + '/routes/components');
  }
}
