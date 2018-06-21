import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { App, DockerHubApp, DockerHubTag } from './app';
import { Cml } from './cml';
import { Result } from '../result';

import { environment } from '../../environments/environment';

@Injectable()
export class AppService {
    private company = 'fhgaisec';

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

    installApp(appId: string, tag: string): Observable<string> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        return this.http.post<string>(environment.apiURL + '/app/install',
            { image: this.company + '/' + appId + ':' + tag }, { headers })
            .catch((error: any) => Observable.throw(error || 'Server error'));
    }

    getTags(appName: string): Observable<Array<DockerHubTag>> {
        const url: string = 'http://ids.aisec.fraunhofer.de/cors/https://hub.docker.com/v2/repositories/'
            + this.company
            + '/'
            + appName
            + '/tags/';
        const result = this.http.get(url)
            .map(resp => resp['results'] as Array<DockerHubTag>);

        return result;
    }

    getCmlVersion(): Observable<Cml> {
        return this.http.get<Cml>(environment.apiURL + '/app/cml_version');
    }

    searchApps(term: string): Observable<Array<DockerHubApp>> {
        const limit = 100;
        const url: string = 'http://ids.aisec.fraunhofer.de/cors/https://hub.docker.com/v2/repositories/'
            + this.company
            + '/\?page_size\='
            + limit;

        return this.http.get(url)
            .map(res => res['results'] as Array<DockerHubApp>)
            .map(apps => apps.filter(app => app.name.match(term)));
    }

    getAllTags(term: string): Observable<Array<DockerHubApp>> {
        const searchedApps: Observable<Array<DockerHubApp>> = this.searchApps(term);

        return searchedApps.map(apps => {
            for (const app of apps) {
                this.getTags(app.name)
                    .forEach(x => {
                        app.tags = x;

                        return app;
                    });
            }

            return apps;
        });
    }
}
