import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {RequestOptions, Request, RequestMethod} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';

import { Connection } from './connection';
import { environment } from '../../environments/environment';

@Injectable()
export class ConnectionService {

  constructor(private http: Http) { }

  getIncommingConnections() {
    return this.http.get(environment.apiURL + '/connections/list')
               .map(response => {
                 return response.json() as Connection[];
               });
  }


}
