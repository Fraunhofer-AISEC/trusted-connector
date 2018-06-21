import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { Settings } from './settings.interface';
import { TermsOfService } from './terms-of-service.interface';
import { ApplicationHttpClient, HTTP_INJECTION_TOKEN } from '../application-http-client.service';

@Injectable()
export class SettingsService {
  constructor(@Inject(HTTP_INJECTION_TOKEN) private http: ApplicationHttpClient) { }

  getSettings(): Observable<Settings> {
    return this.http.get<Settings>('/config');
  }

  getToS(uri: string): Observable<TermsOfService> {
    return this.http.get<TermsOfService>('/certs/acme_tos', {
      cacheTTL: 60,
      params: {uri}
    });
  }

  store(model: Settings): Observable<string> {
    return this.http.post('/config', model, { responseType: 'text' });
  }
}
