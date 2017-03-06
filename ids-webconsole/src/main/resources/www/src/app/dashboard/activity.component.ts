import { Component, OnInit, ViewContainerRef, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

import { SensorService } from '../sensor/sensor.service';

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

  constructor(private sensorService: SensorService) {

  }

  ngOnInit(): void {
    this.chart = c3.generate({
        data: {
          columns: [
              ['data', 0]
            ],
          type: 'gauge',
        },
        color: {
          pattern: ['#209e91']
        },
        gauge: {
          label: {
            format: function(value, perc) { return Math.round((perc * 170)) + ' rpm'; }
          },
        },
        legend: {
          show: false
        }
    });
    this.chart.internal.config.gauge_max = 170;

    let values = [];
    var avg = 0;

    this.sensorService.getValueObservable().subscribe({
       next: event => {
         values.push(event.data);

         if(values.length > 5) {
           values.shift();
         }
         var newAvg = Math.round(values.reduce(function(p,c,i,a){return p + (c/a.length)},0));

         if(newAvg != avg && !document.hidden) {
           avg = newAvg;
           this.chart.load({
           columns: [
             ['data', avg],
           ]
          });
        }
       },
       error: err => console.error('something wrong occurred: ' + err)
   });

    /*this.subscriptions.push(Observable
      .timer(0, 2000)
      .subscribe(() => {
        this.chart.flow({
          columns: [
            ['data1', Math.random()* 400],
          ]
        });
      }));*/
  }

  ngOnDestroy(): void {
    console.log('Unsubscribing...');
    for(let subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }
}
