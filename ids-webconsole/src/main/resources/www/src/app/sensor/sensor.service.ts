import { Injectable, NgZone } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/Rx';
import { Observable } from 'rxjs/Observable';

import { environment } from '../../environments/environment';

declare var EventSource: any;

@Injectable()
export class SensorService {

  private powerObservable;
  private rpmObservable;

  constructor(private http: Http) {
    console.log('constructor SensorService');
    this.powerObservable = Observable.timer(0, 1000).mergeMap(() => this.getCurrentPower());
    this.rpmObservable = Observable.timer(0, 1000).mergeMap(() => this.getCurrentRPM());
  }

  getCurrentPower() {
    return this.http.get('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentpower/value/')
        .map(response => {
          return +response.json().currentPower as number;
        });
  }

  getCurrentRPM() {
    return this.http.get('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentrpm/value/')
        .map(response => {
          return +response.json().sensorReading as number;
        });
  }

  getPowerObservable() {
    return this.powerObservable;
  }

  getRPMObservable() {
    return this.rpmObservable;
  }

}
