import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {RequestOptions, Request, RequestMethod} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';

import { IncomingConnection } from './connections';
import { OutgoingConnection } from './connections';
import { environment } from '../../environments/environment';

@Injectable()
export class ConnectionInOutService {

  constructor(private http: Http) { }


  getIncomingConnections() {
    return this.http.get(environment.apiURL + '/connections/listincoming')
               .map(response => {
                 return response.json() as IncomingConnection[];
               });
  }

  getOutgoingConnections() {
    return this.http.get(environment.apiURL + '/connections/listoutgoing')
               .map(response => {
                 return response.json() as OutgoingConnection[];
               });
  }


}
