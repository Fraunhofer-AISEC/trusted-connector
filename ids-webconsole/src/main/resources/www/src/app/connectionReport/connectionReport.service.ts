import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/observable';
import { map, retryWhen } from 'rxjs/operators';

import { Endpoint, IncomingConnection, OutgoingConnection } from './connections';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class ConnectionReportService {

  constructor(private http: HttpClient) { }

  getEndpoints(): Observable<Array<Endpoint>> {
    return this.http.get(environment.apiURL + '/connections/endpoints')
      .map(response => response as Array<Endpoint>);
  }

  getIncomingConnections(): Observable<Array<IncomingConnection>> {
    return this.http.get(environment.apiURL + '/connections/incoming')
      .map(response => response as Array<IncomingConnection>);
  }

  getOutgoingConnections(): Observable<Array<OutgoingConnection>> {
    return this.http.get(environment.apiURL + '/connections/outgoing')
      .map(response => response as Array<OutgoingConnection>);
  }

}
