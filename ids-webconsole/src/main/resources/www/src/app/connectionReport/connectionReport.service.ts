import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { map, retryWhen } from 'rxjs/operators';

import { IncomingConnection } from './connections';
import { OutgoingConnection } from './connections';
import { Endpoint } from './connections';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class ConnectionReportService {

  constructor(private http: HttpClient) { }

  getEndpoints(): Observable<Endpoint[]> {
    return this.http.get(environment.apiURL + '/connections/endpoints')
      .map(response => response as Endpoint[]);
  }

  getIncomingConnections(): Observable<IncomingConnection[]> {
    return this.http.get(environment.apiURL + '/connections/incoming')
      .map(response => response as IncomingConnection[]);
  }

  getOutgoingConnections(): Observable<OutgoingConnection[]> {
    return this.http.get(environment.apiURL + '/connections/outgoing')
      .map(response => response as OutgoingConnection[]);
  }

}
