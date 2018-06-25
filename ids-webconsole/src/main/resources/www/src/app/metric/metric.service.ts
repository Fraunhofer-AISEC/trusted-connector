import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { interval, Observable } from 'rxjs';
import { mergeMap, takeWhile } from 'rxjs/operators';

@Injectable()
export class MetricService implements OnDestroy {
    private metricObservable: Observable<Array<String>>;
    private alive = false;

    constructor(private http: HttpClient) {
        this.metricObservable = interval(1000)
            .pipe(
                takeWhile(() => this.alive),
                mergeMap(() => this.getMetric())
            );
        this.alive = true;
    }

    ngOnDestroy(): void {
        this.alive = false;
    }

    getMetric(): Observable<Array<String>> {
        return this.http.get<Array<String>>(environment.apiURL + '/metric/get');
    }

    getMetricObservable(): Observable<Array<String>> {
        return this.metricObservable;
    }
}
