import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Result } from '../result';

import { Route, RouteComponent } from './route';
import { ValidationInfo } from './validation-info';

@Injectable()
export class RouteService {
  constructor(private readonly httpClient: HttpClient) { }

  public getRoute(routeId: string): Observable<Route> {
    return this.httpClient.get(environment.apiURL + '/routes/get/' + routeId) as Observable<Route>;
  }

  public getValidationInfo(routeId: string): Observable<ValidationInfo> {
    return this.httpClient.get(environment.apiURL + '/routes/validate/' + routeId) as Observable<ValidationInfo>;
  }

  public getRoutes(): Observable<Route[]> {
    return this.httpClient.get(environment.apiURL + '/routes/list') as Observable<Route[]>;
  }

  public stopRoute(routeId: string): Observable<Result> {
    // Stop Camel route
    return this.httpClient.get(environment.apiURL + '/routes/stoproute/' + routeId) as Observable<Result>;
  }

  public startRoute(routeId: string): Observable<Result> {
    // Start Camel route
    return this.httpClient.get(environment.apiURL + '/routes/startroute/' + routeId) as Observable<Result>;
  }

  public listComponents(): Observable<RouteComponent[]> {
    return this.httpClient.get<RouteComponent[]>(environment.apiURL + '/routes/components');
  }
}
