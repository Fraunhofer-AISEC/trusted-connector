
import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { Observable, timer } from 'rxjs';
import { map, mergeMap, takeWhile } from 'rxjs/operators';

@Injectable()
export class SensorService implements OnDestroy {
  private readonly powerObservable;
  private readonly rpmObservable;
  private alive = true;

  constructor(private readonly http: HttpClient) {
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

  public ngOnDestroy(): void {
    this.alive = false;
  }

  public getCurrentPower(): Observable<number> {
    return this.http.get<any>('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentpower/value/')
      .pipe(map(response => response.currentPower));
  }

  public getCurrentRPM(): Observable<number> {
    return this.http.get<any>('http://iot-connector1.netsec.aisec.fraunhofer.de:8080/sensordataapp/currentrpm/value/')
      .pipe(map(response => response.sensorReading));
  }

  public getPowerObservable(): Observable<number> {
    return this.powerObservable;
  }

  public getRPMObservable(): Observable<number> {
    return this.rpmObservable;
  }

}
