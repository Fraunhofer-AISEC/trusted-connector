
import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { map, mergeMap, takeWhile } from 'rxjs/operators';

@Injectable()
export class SensorService implements OnDestroy {
  private powerObservable;
  private rpmObservable;
  private alive = true;

  constructor(private http: HttpClient) {
    // console.log('constructor SensorService');
    this.powerObservable = timer(0, 1000)
      .pipe(
        takeWhile(() => this.alive),
        mergeMap(() => this.getCurrentPower())
      );
    this.rpmObservable = timer(0, 1000)
      .pipe(
        takeWhile(() => this.alive),
        mergeMap(() => this.getCurrentRPM())
      );
  }

  ngOnDestroy(): void {
    this.alive = false;
  }

  getCurrentPower(): Observable<number> {
    return this.http.get<any>('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentpower/value/')
      .pipe(map(response => response.currentPower));
  }

  getCurrentRPM(): Observable<number> {
    return this.http.get<any>('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentrpm/value/')
      .pipe(map(response => response.sensorReading));
  }

  getPowerObservable(): Observable<number> {
    return this.powerObservable;
  }

  getRPMObservable(): Observable<number> {
    return this.rpmObservable;
  }

}
