import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import 'rxjs/add/operator/map';
import {  environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { IntervalObservable } from 'rxjs/observable/IntervalObservable';
import 'rxjs/add/operator/takeWhile';
import { OnDestroy } from '@angular/core/src/metadata/lifecycle_hooks';

@Injectable()
export class MetricService implements OnDestroy {
    private metricObservable: Observable<Object>;
    private alive = false;

    constructor( private http: HttpClient ) {
        this.metricObservable = IntervalObservable.create(5000)
            .takeWhile(() => this.alive)
            .map(() => {
                return this.getMetric();
            });
        this.alive = true;
    }

    ngOnDestroy() {
        this.alive = false;
    }

    getMetric() {
        return this.http.get( environment.apiURL + '/metric/get' );
    }

    getMetricObservable(): Observable<any> {
        return this.metricObservable;
    }
}
