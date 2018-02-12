import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/observable';

import { Endpoint, IncomingConnection, OutgoingConnection } from './connections';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class ConnectionReportService {

  constructor(private http: HttpClient) { }

  getEndpoints(): Observable<Array<Endpoint>> {
    return this.http.get<Array<Endpoint>>(environment.apiURL + '/connections/endpoints');
  }

  getIncomingConnections(): Observable<Array<IncomingConnection>> {
    return this.http.get<Array<IncomingConnection>>(environment.apiURL + '/connections/incoming');
  }

  getOutgoingConnections(): Observable<Array<OutgoingConnection>> {
    return this.http.get<Array<OutgoingConnection>>(environment.apiURL + '/connections/outgoing');
  }

}
