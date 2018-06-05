import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { Settings } from './settings.interface';
import { environment } from '../../environments/environment';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable()
export class SettingsService {
  constructor(private http: HttpClient) { }

  getSettings(): Observable<Settings> {
    return this.http.get<Settings>(environment.apiURL + '/config');
  }

  store(model: Settings): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    return this.http.post(environment.apiURL + '/config', model, {
      headers,
      responseType: 'text'
    });
  }

}
