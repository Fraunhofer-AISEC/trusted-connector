import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {RequestOptions, Request, RequestMethod} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';

import { IncomingConnection } from './connections';
import { OutgoingConnection } from './connections';
import { Endpoint } from './connections';
import { environment } from '../../environments/environment';

@Injectable()
export class ConnectionReportService {

  constructor(private http: Http) { }

  getEndpoints() {
    return this.http.get(environment.apiURL + '/connections/endpoints')
      .map(response => response.json() as Endpoint[]);
  }

  getIncomingConnections() {
    return this.http.get(environment.apiURL + '/connections/incoming')
      .map(response => response.json() as IncomingConnection[]);
  }

  getOutgoingConnections() {
    return this.http.get(environment.apiURL + '/connections/outgoing')
      .map(response => response.json() as OutgoingConnection[]);
  }


}
