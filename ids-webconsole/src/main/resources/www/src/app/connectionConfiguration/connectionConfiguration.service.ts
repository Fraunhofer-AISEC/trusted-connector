import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import {RequestOptions, Request, RequestMethod} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';

import { IncomingConnection } from '../connectionReport/connections';
import { OutgoingConnection } from '../connectionReport/connections';
import { environment } from '../../environments/environment';

import { Configuration } from './configuration.interface';
import { Settings } from './settings.interface';

@Injectable()
export class ConnectionConfigurationService {
    constructor(private http: Http) { }

    store(model: Configuration): Observable<Response> {
        let headers = new Headers({ 'Content-Type': 'application/json' });
        let params: URLSearchParams = new URLSearchParams();
        params.set('connection', model.connection as string);
        
        let requestOptions = new RequestOptions({ headers: headers });
        requestOptions.search = params;
        
        let result = this.http.post(environment.apiURL + '/config/setconnectionconfigs', JSON.stringify(model.settings), requestOptions);
  
        return result;
    }

    getConfiguration(connection: string)  {
      let params: URLSearchParams = new URLSearchParams();
      params.set('connection', connection);
      
      let requestOptions = new RequestOptions();
      requestOptions.search = params;
    
      return this.http.get(environment.apiURL + '/config/getconnectionconfigs/' , requestOptions)
        .map(res => {
          return JSON.parse(JSON.stringify(res.text())) as Configuration
          });
    }

    getAllConfiguration(): Observable<Configuration[]>{
      return this.http.get(environment.apiURL + '/config/getallconnectionconfigs/')
              .map((response: Response) => response.json() as Configuration[]);
              
    }
}