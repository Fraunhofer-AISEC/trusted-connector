import { Component, OnInit, ViewContainerRef, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';

import {  SubscriptionComponent } from '../subscription.component';

import { SensorService } from '../sensor/sensor.service';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/timer';

import * as d3 from 'd3';

@Component({
  selector: 'activity',
  template: `
  <div class="mdl-grid">
    <div class="mdl-card mdl-cell mdl-cell--3-col">
      <div class="chart-gauge2"></div>
      <div style="text-align: center; font-size: 15pt">{{power | number: '1.2-2'}} % Power</div>
    </div>
    <div class="mdl-card mdl-cell mdl-cell--3-col">
      <div class="chart-gauge"></div>
      <div style="text-align: center; font-size: 15pt">{{rpm | number: '1.2-2'}} rpm</div>
    </div>
    <div class="mdl-card mdl-cell mdl-cell--6-col">
      <div id="timeChart"></div>
    </div>
  </div>`
})
export class ActivityComponent extends SubscriptionComponent implements OnInit {
  public power = 0;
  public rpm = 0;

  private needle: any;
  private needle2: any;

  constructor(private sensorService: SensorService) {
    super();
  }

  ngOnInit(): void {
    this.needle = this.createNeedle(d3.select('.chart-gauge'));
    this.needle2 = this.createNeedle(d3.select('.chart-gauge2'));

    this.createTimeChart();

    this.subscriptions.push(this.sensorService.getRPMObservable().subscribe(rpm => {
      this.rpm = rpm;
      this.needle.moveTo(this.rpm / 200);
    }));
    this.subscriptions.push(this.sensorService.getPowerObservable().subscribe(power => {
      this.power = power;
      this.needle2.moveTo(this.power / 100);

      let filled = d3.select('.chart-gauge2').select('svg').select('g').select('.chart-filled');

      if (this.power >= 60) {
        filled.attr('class', 'chart-filled chart-filled-error');
      } else {
        filled.attr('class', 'chart-filled');
      }
    }));
  }

  createTimeChart() {
    let limit = 240 * 1,
      duration: any = 750,
      now: any = new Date(Date.now() - duration);

    let el = d3.select('#timeChart');

    let width = (el[0][0] as any).offsetWidth,
      height = 160;

    let groups = {
      current: {
        value: 0,
        color: 'black',
        data: d3.range(limit).map(function () {
          return 0;
        })
      },
    };

    let x: any = d3.time.scale()
      .domain([now - (limit - 2), now - duration])
      .range([0, width]);

    let y: any = d3.scale.linear()
      .domain([0, 200])
      .range([height, 0]);

    /*var line = d3.svg.line()
        .interpolate('basis')
        .x(function(d, i) {
            return x(now - (limit - 1 - i) * duration)
        })
        .y(function(d) {
            return y(d)
        })*/

    let area = d3.svg.area()
      .interpolate('basis')
      .x(function (d, i) {
        return x(now - (limit - 1 - i) * duration);
      })
      .y0(height)
      .y1(function (d) {
        return y(d);
      });

    let svg = el.append('svg')
      .attr('class', 'chart')
      .attr('width', width)
      .attr('height', height + 50);

    let paths = svg.append('g');

    let axis = svg.append('g')
      .attr('class', 'x axis')
      .attr('transform', 'translate(0,' + height + ')')
      .call(x.axis = d3.svg.axis().scale(x).orient('bottom'));

    for (let name in groups) {
      if (groups.hasOwnProperty(name)) {
        let group = groups[name];
        group.path = paths.append('path')
          .data([group.data])
          .attr('class', name + ' group')
          .style('stroke', group.color);

        group.area = paths.append('path')
          .datum(group.data)
          .attr('class', 'area')
          .attr('d', area);
      }
    }

    let tick = () => {
      now = new Date();

      // Add new values
      for (let name in groups) {
        if (groups.hasOwnProperty(name)) {
          let group = groups[name];
          // group.data.push(group.value) // Real values arrive at irregular intervals
          group.data.push(this.rpm);
          // group.path.attr('d', line)
          group.area.attr('d', area);
        }
      }

      // Shift domain
      x.domain([now - (limit - 2) * duration, now - duration]);

      // Slide x-axis left
      axis.transition()
        .duration(duration)
        .ease('linear')
        .call(x.axis);

      // Slide paths left
      paths.attr('transform', null)
        .transition()
        .duration(duration)
        .ease('linear')
        .attr('transform', 'translate(' + x(now - (limit - 1) * duration) + ')');
      //                .each('end', tick)

      // Remove oldest data point from each group
      for (let name in groups) {
        if (groups.hasOwnProperty(name)) {
          let group = groups[name];
          group.data.shift();
        }
      }
      setTimeout(tick, 1000);
    };
    tick();
  }

  createNeedle(el) {

    let barWidth, chart, chartInset, degToRad, repaintGauge,
      height, margin, numSections, padRad, percToDeg, percToRad,
      percent, radius, sectionIndx, svg, totalPercent, width;

    numSections = 1;
    let sectionPerc = 1 / numSections / 2;
    padRad = 0.025;
    chartInset = 10;

    // Orientation of gauge:
    totalPercent = .75;

    margin = {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    };

    width = 300;
    height = 300;
    radius = Math.min(width, height) / 2;
    barWidth = 40 * width / 300;

    /*
      Utility methods
    */
    percToDeg = function (perc) {
      return perc * 360;
    };

    percToRad = function (perc) {
      return degToRad(percToDeg(perc));
    };

    degToRad = function (deg) {
      return deg * Math.PI / 180;
    };

    // Create SVG element
    svg = el.append('svg').attr('width', 300).attr('height', 200);

    // Add layer for the panel
    // chart = svg.append('g').attr('transform', "translate(" + ((width + margin.left) / 2) + ", " + ((height + margin.top) / 2) + ")");
    chart = svg.append('g').attr('transform', 'translate(160, 150)');
    chart.append('path').attr('class', 'arc chart-filled');
    chart.append('path').attr('class', 'arc chart-empty');

    let arc2 = d3.svg.arc().outerRadius(radius - chartInset).innerRadius(radius - chartInset - barWidth);
    let arc1 = d3.svg.arc().outerRadius(radius - chartInset).innerRadius(radius - chartInset - barWidth);

    repaintGauge = function (perc) {
      let next_start = totalPercent;
      let arcStartRad = percToRad(next_start);
      let arcEndRad = arcStartRad + percToRad(perc / 2);
      next_start += perc / 2;

      arc1.startAngle(arcStartRad).endAngle(arcEndRad);

      arcStartRad = percToRad(next_start);
      arcEndRad = arcStartRad + percToRad((1 - perc) / 2);

      arc2.startAngle(arcStartRad + padRad).endAngle(arcEndRad);


      chart.select('.chart-filled').attr('d', arc1);
      chart.select('.chart-empty').attr('d', arc2);

    };


    let Needle = (function () {

      /**
      * Helper function that returns the `d` value
      * for moving the needle
      **/
      let recalcPointerPos = function (perc) {
        let centerX, centerY, leftX, leftY, rightX, rightY, thetaRad, topX, topY;
        thetaRad = percToRad(perc / 2);
        centerX = 0;
        centerY = 0;
        topX = centerX - this.len * Math.cos(thetaRad);
        topY = centerY - this.len * Math.sin(thetaRad);
        leftX = centerX - this.radius * Math.cos(thetaRad - Math.PI / 2);
        leftY = centerY - this.radius * Math.sin(thetaRad - Math.PI / 2);
        rightX = centerX - this.radius * Math.cos(thetaRad + Math.PI / 2);
        rightY = centerY - this.radius * Math.sin(thetaRad + Math.PI / 2);
        return 'M ' + leftX + ' ' + leftY + ' L ' + topX + ' ' + topY + ' L ' + rightX + ' ' + rightY;
      };

      // tslint:disable-next-line:no-shadowed-variable
      let Needle = function (el) {
        this.el = el;
        this.len = width / 3;
        this.radius = this.len / 6;
      };

      Needle.prototype.render = function () {
        this.el.append('circle').attr('class', 'needle-center').attr('cx', 0).attr('cy', 0).attr('r', this.radius);
        return this.el.append('path').attr('class', 'needle').attr('d', recalcPointerPos.call(this, 0));
      };

      Needle.prototype.moveTo = function (perc) {
        let self,
          oldValue = this.perc || 0;

        this.perc = perc;
        self = this;

        this.el.transition().delay(0).ease('quad').duration(0).select('.needle').tween('progress', function () {
          return function (percentOfPercent) {
            let progress = percentOfPercent * perc;

            repaintGauge(progress);
            return d3.select(this).attr('d', recalcPointerPos.call(self, progress));
          };
        });

      };

      return Needle;

    })();

    let needle = new Needle(chart);
    needle.render();

    return needle;
  }
}
