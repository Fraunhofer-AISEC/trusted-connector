import { Component, OnInit, ViewContainerRef, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';

import { SubscriptionComponent } from '../subscription.component';

import { SensorService } from '../sensor/sensor.service';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/timer';

import * as d3 from 'd3';
import * as c3 from 'c3';

@Component({
  selector: 'activity',
  template: `<div id="chart"></div><div id="timeChart"></div>`
})
export class ActivityComponent extends SubscriptionComponent implements OnInit {
  private chart: any;
  private timeChart: any = {};

  values: any[] = [];
  avg: number = 0;

  constructor(private sensorService: SensorService) {
    super();
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

    this.createTimeChart();

    Observable
      .timer(0, 1000)
      .subscribe(window => {
        this.avg = Math.round(this.values.reduce(function(p,c,i,a){return p + (c/a.length)},0));

        this.chart.load({
          columns: [
            ['data', this.avg],
          ]
        });

        //this.updateTimeChart();
      });

    let sub = this.sensorService.getValueObservable().subscribe({
       next: event => {
         this.values.push(event.data);

         if(this.values.length > 5) {
           this.values.shift();
         }
      },
       error: err => console.error('something wrong occurred: ' + err)
   });
   this.subscriptions.push(sub);

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

  createTimeChart() {

    var limit = 60 * 1,
        duration:any = 750,
        now:any = new Date(Date.now() - duration)

    var width = 500,
        height = 200

    var groups = {
        current: {
            value: 0,
            color: 'orange',
            data: d3.range(limit).map(function() {
                return 0
            })
        },
    }

    var x:any = d3.time.scale()
        .domain([now - (limit - 2), now - duration])
        .range([0, width])

    var y:any = d3.scale.linear()
        .domain([0, 100])
        .range([height, 0])

    var line = d3.svg.line()
        .interpolate('basis')
        .x(function(d, i) {
            return x(now - (limit - 1 - i) * duration)
        })
        .y(function(d) {
            return y(d)
        })

    var svg = d3.select('#timeChart').append('svg')
        .attr('class', 'chart')
        .attr('width', width)
        .attr('height', height + 50)

    var axis = svg.append('g')
        .attr('class', 'x axis')
        .attr('transform', 'translate(0,' + height + ')')
        .call(x.axis = d3.svg.axis().scale(x).orient('bottom'))

    var paths = svg.append('g')

    for (var name in groups) {
        var group = groups[name]
        group.path = paths.append('path')
            .data([group.data])
            .attr('class', name + ' group')
            .style('stroke', group.color)
    }

    let tick = () => {
      now = new Date()

        // Add new values
        for (var name in groups) {
            var group = groups[name]
            //group.data.push(group.value) // Real values arrive at irregular intervals
            group.data.push(this.avg)
            group.path.attr('d', line)
        }

        // Shift domain
        x.domain([now - (limit - 2) * duration, now - duration])

        // Slide x-axis left
        axis.transition()
            .duration(duration)
            .ease('linear')
            .call(x.axis)

        // Slide paths left
        paths.attr('transform', null)
            .transition()
            .duration(duration)
            .ease('linear')
            .attr('transform', 'translate(' + x(now - (limit - 1) * duration) + ')')
//                .each('end', tick)

        // Remove oldest data point from each group
        for (var name in groups) {
            var group = groups[name]
            group.data.shift()
        }
        setTimeout(tick, 1000);
    };
    tick();
  }
}
