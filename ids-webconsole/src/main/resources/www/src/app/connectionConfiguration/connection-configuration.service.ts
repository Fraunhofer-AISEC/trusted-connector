import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { URLSearchParams } from '@angular/http';
import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { RequestOptions, Request, RequestMethod } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { map } from 'rxjs/operators';

import { IncomingConnection } from '../connectionReport/connections';
import { OutgoingConnection } from '../connectionReport/connections';
import { environment } from '../../environments/environment';

import { Configuration } from './configuration';
import { Settings } from './settings.interface';

@Injectable()
export class ConnectionConfigurationService {
  constructor(private http: Http) { }

  storeConfiguration(config: Configuration): Observable<Response> {
    return this.http.post(environment.apiURL + '/config/connectionConfigs/' + encodeURIComponent(config.connection),
      JSON.stringify(config.settings), { headers: new Headers({ 'Content-Type': 'application/json' }) });
  }

  getConfiguration(connection: string): Observable<Configuration> {
    return this.http.get(environment.apiURL + '/config/connectionConfigs/' + encodeURIComponent(connection))
      .map(res => new Configuration(connection, res.json() as Settings));
  }

  getAllConfiguration(): Observable<Configuration[]> {
    return this.http.get(environment.apiURL + '/config/connectionConfigs')
      .map(res => {
        let configMap = res.json();
        return Object.keys(configMap).map(key => new Configuration(key, configMap[key]));
      });
  }
}
