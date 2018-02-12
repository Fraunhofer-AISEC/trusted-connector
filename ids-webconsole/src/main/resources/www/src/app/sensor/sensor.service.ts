import { Injectable, NgZone, OnDestroy } from '@angular/core';

import { Observable } from 'rxjs/observable';
import 'rxjs/add/observable/timer';

import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';

import 'rxjs/add/operator/takeWhile';
import { mergeMap } from 'rxjs/operators';

declare var EventSource: any;

@Injectable()
export class SensorService implements OnDestroy {
  private powerObservable;
  private rpmObservable;
  private alive = true;

  constructor(private http: HttpClient) {
    // console.log('constructor SensorService');
    this.powerObservable = Observable.timer(0, 1000)
      .takeWhile(() => this.alive)
      .mergeMap(() => this.getCurrentPower());
    this.rpmObservable = Observable.timer(0, 1000)
      .takeWhile(() => this.alive)
      .mergeMap(() => this.getCurrentRPM());
  }

  ngOnDestroy(): void {
    this.alive = false;
  }

  getCurrentPower(): Observable<number> {
    return this.http.get<any>('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentpower/value/')
        .map(response => response.currentPower);
  }

  getCurrentRPM(): Observable<number> {
    return this.http.get<any>('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentrpm/value/')
        .map(response => response.sensorReading);
  }

  getPowerObservable(): Observable<number> {
    return this.powerObservable;
  }

  getRPMObservable(): Observable<number> {
    return this.rpmObservable;
  }

}
