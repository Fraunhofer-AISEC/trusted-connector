import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { interval, Observable } from 'rxjs';
import { mergeMap, takeWhile } from 'rxjs/operators';

import { environment } from '../../environments/environment';

@Injectable()
export class MetricService implements OnDestroy {
    private readonly metricObservable: Observable<string[]>;
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

    public getMetric(): Observable<string[]> {
        return this.http.get<string[]>(environment.apiURL + '/metric/get');
    }

    public getMetricObservable(): Observable<string[]> {
        return this.metricObservable;
    }
}
