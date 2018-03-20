import { Component, ElementRef, Injectable, Input, ViewChild } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { map } from 'rxjs/operators';

import { IncomingConnection, OutgoingConnection } from './../connectionReport/connections';
import { environment } from '../../environments/environment';

import { Configuration } from './configuration';
import { Settings } from './settings.interface';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable()
export class ConnectionConfigurationService {
  constructor(private http: HttpClient) { }

  storeConfiguration(config: Configuration): Observable<string> {
    return this.http.post(environment.apiURL + '/config/connectionConfigs/' + encodeURIComponent(config.connection),
      JSON.stringify(config.settings), {
        headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
        responseType: 'text'
      });
  }

  getConfiguration(connection: string): Observable<Configuration> {
    return this.http.get<Settings>(environment.apiURL + '/config/connectionConfigs/' + encodeURIComponent(connection))
      .map(res => new Configuration(connection, res));
  }

  getAllConfiguration(): Observable<Array<Configuration>> {
    return this.http.get<object>(environment.apiURL + '/config/connectionConfigs')
      .map(configMap => Object.keys(configMap)
        .map(key => new Configuration(key, configMap[key])));
  }
}
