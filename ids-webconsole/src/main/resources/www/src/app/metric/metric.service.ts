import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import { environment } from '../../environments/environment';


@Injectable()
export class MetricService {
    private metricObservable;

    constructor(private http: Http) {
        this.metricObservable = Observable
            .timer(0, 2000)
            .flatMap(() => { return this.getMetric(); });
    }

    getMetric() {
        return this.http.get(environment.apiURL + "/metric/get")
            .map(response => {
                var result = response.json();
                console.log(result);
                return result;
            });
    }
    
    getMetricObservable() : Observable<any> {
        return this.metricObservable;
    }

}
