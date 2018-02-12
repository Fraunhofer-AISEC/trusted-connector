import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs/observable';
import { IntervalObservable } from 'rxjs/observable/IntervalObservable';
import 'rxjs/add/operator/takeWhile';

import { mergeMap } from 'rxjs/operators';

@Injectable()
export class MetricService implements OnDestroy {
    private metricObservable: Observable<Array<String>>;
    private alive = false;

    constructor(private http: HttpClient) {
        this.metricObservable = IntervalObservable.create(5000)
            .takeWhile(() => this.alive)
            .mergeMap(() => this.getMetric());
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
