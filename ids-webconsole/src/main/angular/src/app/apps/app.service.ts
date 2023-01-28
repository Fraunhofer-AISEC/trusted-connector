import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../environments/environment';
import { Result } from '../result';

import { App } from './app';
import { Cml } from './cml';

@Injectable()
export class AppService {

    constructor(private readonly http: HttpClient) {
    }

    public getApps(): Observable<App[]> {
        return this.http.get<App[]>(environment.apiURL + '/app/list');
    }

    public stopApp(appId: string): Observable<Result> {
        return this.http.get<Result>(environment.apiURL + '/app/stop/' + encodeURIComponent(appId));
    }

    public startApp(appId: string, key?: string): Observable<Result> {
        let url = environment.apiURL + '/app/start/' + encodeURIComponent(appId);
        if (key !== undefined && key !== null) {
            url += '/' + encodeURIComponent(key);
        }
        return this.http.get<Result>(url);
    }

    public wipeApp(appId: string): Observable<void> {
        return this.http.get<void>(environment.apiURL + '/app/wipe?containerId=' + encodeURIComponent(appId))
            .pipe(catchError((error: any) => throwError(new Error(error || 'Server error'))));
    }

    public installApp(app: App): Observable<void> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        return this.http.post<void>(environment.apiURL + '/app/install', app, { headers })
            .pipe(catchError((error: any) => throwError(new Error(error || 'Server error'))));
    }

    public getCmlVersion(): Observable<Cml> {
        return this.http.get<Cml>(environment.apiURL + '/app/cml_version');
    }

    public searchApps(term: string): Observable<App[]> {
        const headers = new HttpHeaders({ 'Content-Type': 'text/plain' });

        return this.http.post<App[]>(environment.apiURL + '/app/search',
            term, {headers});
    }

}
