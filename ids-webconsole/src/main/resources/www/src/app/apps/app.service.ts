import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { App, AppSearchTerm } from './app';
import { Cml } from './cml';
import { Result } from '../result';

import { environment } from '../../environments/environment';

import { catchError } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';

@Injectable()
export class AppService {

    constructor(private http: HttpClient) {
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

    wipeApp(appId: string): Observable<Result> {
        return this.http.get<Result>(environment.apiURL + '/app/wipe?containerId=' + encodeURIComponent(appId))
            .pipe(catchError((error: any) => throwError(new Error(error || 'Server error'))));
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

    /*getAllTags(term: string): Observable<Array<App>> {
        const searchedApps: Observable<Array<App>> = this.searchApps(term);

        return searchedApps.pipe(map(apps => {
            for (const app of apps) {
                this.getTags(app.name)
                    .forEach(x => {
                        app.tags = x;

                        return app;
                    });
            }

            return apps;
        }));
    }*/
}
