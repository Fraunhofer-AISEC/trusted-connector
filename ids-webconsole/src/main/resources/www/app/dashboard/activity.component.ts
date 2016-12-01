import { Component, OnInit, ViewContainerRef, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/timer';

import * as d3 from 'd3';
import * as c3 from 'c3';

@Component({
  selector: 'activity',
  template: `<div id="chart">`
})
export class ActivityComponent implements OnInit {
  private subscriptions: Subscription[] = [];

  private chart: any;

  constructor() {
  }

  ngOnInit(): void {
    this.chart = c3.generate({
        data: {
            columns: [
                ['data1', 300, 350, 300, 0, 0, 0],
                ['data2', 130, 100, 140, 200, 150, 50]
            ],
            types: {
                data1: 'area',
                data2: 'area-spline'
            }
        }
    });

    this.subscriptions.push(Observable
      .timer(0, 3000)
      .subscribe(() => {
        this.chart.flow({
          columns: [
            ['data1', Math.random()* 400],
            ['data2', Math.random()* 400],
          ]
        });
      }));
  }

  ngOnDestroy(): void {
    console.log("Unsubscribing...");
    for(let subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }
}
