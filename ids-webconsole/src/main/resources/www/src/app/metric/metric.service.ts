import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import 'rxjs/add/operator/map';
import {  environment } from '../../environments/environment';
import { Observable } from "rxjs";
import { IntervalObservable } from "rxjs/observable/IntervalObservable";
import 'rxjs/add/operator/takeWhile';

@Injectable()
export class MetricService {
    private metricObservable;
    private alive = false;

    constructor( private http: HttpClient ) {
        this.metricObservable = IntervalObservable.create( 5000 )
            .takeWhile(() => this.alive )
            .flatMap(() => {
                return this.http.get( environment.apiURL + "/metric/get" );
            } );
        this.alive = true;
    }
    
    ngOnDestroy() {
        this.alive = false; // switches your IntervalObservable off
    }

    getMetric() {
        return this.http.get( environment.apiURL + "/metric/get" );
    }

    getMetricObservable(): Observable<any> {
        return this.metricObservable;
    }
}