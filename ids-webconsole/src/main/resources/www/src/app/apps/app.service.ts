import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../environments/environment';
import { Result } from '../result';

import { App, AppSearchTerm } from './app';
import { Cml } from './cml';

@Injectable()
export class AppService {

    constructor(private readonly http: HttpClient) {
    }

    getApps(): Observable<Array<App>> {
        return this.http.get<Array<App>>(environment.apiURL + '/app/list/');
    }

    stopApp(appId: string): Observable<Result> {
        return this.http.get<Result>(environment.apiURL + '/app/stop/' + encodeURIComponent(appId));
    }

    startApp(appId: string): Observable<Result> {
        return this.http.get<Result>(environment.apiURL + '/app/start/' + encodeURIComponent(appId));
    }

    installApp(app: App): Observable<string> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        return this.http.post<string>(environment.apiURL + '/app/install',
            { app }, { headers })
            .pipe(catchError((error: any) => throwError(new Error(error || 'Server error'))));
    }

    getCmlVersion(): Observable<Cml> {
        return this.http.get<Cml>(environment.apiURL + '/app/cml_version');
    }

    searchApps(term: string): Observable<Array<App>> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        const searchTerm = new AppSearchTerm();
        searchTerm.searchTerm = term;
        const result = this.http.post<Array<App>>(environment.apiURL + '/app/search',
                searchTerm, {headers});

        return result;
    }

}
