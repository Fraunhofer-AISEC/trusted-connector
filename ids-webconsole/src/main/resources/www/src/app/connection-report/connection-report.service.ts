import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Endpoint } from './endpoint';
import { IncomingConnection } from './incoming-connection';
import { OutgoingConnection } from './outgoing-connection';

@Injectable()
export class ConnectionReportService {

  constructor(private readonly http: HttpClient) { }

  public getEndpoints(): Observable<Endpoint[]> {
    return this.http.get<Endpoint[]>(environment.apiURL + '/connections/endpoints');
  }

  public getIncomingConnections(): Observable<IncomingConnection[]> {
    return this.http.get<IncomingConnection[]>(environment.apiURL + '/connections/incoming');
  }

  public getOutgoingConnections(): Observable<OutgoingConnection[]> {
    return this.http.get<OutgoingConnection[]>(environment.apiURL + '/connections/outgoing');
  }

}
