import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { interval, Observable } from 'rxjs';
import { mergeMap, takeWhile } from 'rxjs/operators';

import { environment } from '../../environments/environment';

@Injectable()
export class MetricService implements OnDestroy {
    private readonly metricObservable: Observable<Array<String>>;
    private alive = false;

    constructor(private readonly http: HttpClient) {
        this.metricObservable = interval(1000)
            .pipe(
                takeWhile(() => this.alive),
                mergeMap(() => this.getMetric())
            );
        this.alive = true;
    }

    public ngOnDestroy(): void {
        this.alive = false;
    }

    public getMetric(): Observable<Array<String>> {
        return this.http.get<Array<String>>(environment.apiURL + '/metric/get');
    }

    public getMetricObservable(): Observable<Array<String>> {
        return this.metricObservable;
    }
}
