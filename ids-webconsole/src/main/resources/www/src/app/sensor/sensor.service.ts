import { Injectable, NgZone } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/map';

import { environment } from '../../environments/environment';

declare var EventSource: any;

@Injectable()
export class SensorService {

  private zone = new NgZone({ enableLongStackTrace: false });
  private valueObservable;

  constructor(private http: Http) {
    console.log("constructor SensorService");
    this.valueObservable = this.createValueObservable();
  }

  createValueObservable() {
    return Observable.create(observer => {
      // TODO: make URL configurable
      let url = 'http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/sensordataelements/events';

      console.log('Creating new EventSource from url: ' + url + '...');

      const eventSource = new EventSource(url);
      eventSource.onmessage = score => this.zone.run(() => observer.next(score));
      eventSource.onerror = error => this.zone.run(() => observer.error(error));
      return () => {
        console.log('Closing EventSource...');
        eventSource.close()
      }
    });
  }

  getValueObservable() {
    return this.valueObservable;
  }

}
