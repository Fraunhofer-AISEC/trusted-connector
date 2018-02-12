import { Component, OnInit, ViewContainerRef, EventEmitter, Output,  AfterViewInit, ViewEncapsulation } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/take';

import { SensorService } from '../sensor/sensor.service';
import { SubscriptionComponent } from '../subscription.component';


import * as d3 from 'd3';
import * as topojson from 'topojson';
import d3_hexbin from 'd3-plugins-dist/dist/mbostock/hexbin/amd';

@Component({
  selector: 'network-graph',
  template: `<div id="network-graph"></div>`,
  styleUrls:  ['./network-graph.component.css'],  // Include CSS classes for land and countries
  encapsulation: ViewEncapsulation.None   // Generate global CSS classes
})
export class NetworkGraphComponent extends SubscriptionComponent implements OnInit, AfterViewInit {
  public power = 0;

  private chart: any;
  private color;
  private hexbin;
  private svg;
  private locations;
  private errorTimer: Observable<number>;
  private isBlinking = false;

  constructor(private sensorService: SensorService) {
    super();
  }

  ngOnInit(): void {
    this.subscriptions.push(this.sensorService.getPowerObservable().subscribe(power => {
      this.power = power;

      if (this.power >= 60) {
        this.errorOn();
      }
    }));
  }

  loadMap(): Promise<any> {
    return new Promise((resolve, reject) => {
      d3.json('data/eu.topo.json', (error, json) => {
        if (error) {
          return reject(error);
        }

        resolve(json);
      });
    });
  }

  loadLocations(): Promise<any[]> {
    return new Promise((resolve, reject) => {
      d3.tsv('data/locations.tsv', (error, json) => {
        if (error) {
          return reject(error);
        }

        resolve(json);
      });
    });
  }

  /*
   * After view has been rendered, add some JavaScript to it. Adapted from http://bl.ocks.org/mbostock/4330486
   */
  async ngAfterViewInit() {
    let width = 960,
        height = 400;

    let self = this;

    this.color = d3.time.scale<string>()
        .domain([2000, 100000])
        .range(['#004D40', '#4DB6AC'])
        .interpolate(d3.interpolateLab);

    this.hexbin = d3_hexbin.default()
        .size([width, height])
        .radius(10);

    let radius = d3.scale.sqrt()
        .domain([0, 12])
        .range([0, 10]);

    let projection = d3.geo.mercator()
        .scale(1720)
        .translate([width - 1000, height + 1600])
        .precision(.1);

    let path = d3.geo.path()
        .projection(projection);

    this.svg = d3.select('#network-graph')
        .append('svg')
        .attr('width', width)
        .attr('height', height);

    let map = await this.loadMap();
    this.locations = await this.loadLocations();

    this.locations.forEach(function(d) {
      let p = projection(d);
      d[0] = p[0], d[1] = p[1];
    });

    // Draw Europe
    this.svg.append('path')
        .datum(topojson.feature(map, map.objects.europe))
        .attr('class', 'land')
        .attr('d', path);

    // Draw countries
    this.svg.append('path')
        .datum(topojson.mesh(map, map.objects.europe, function(a, b) { return a !== b; }))
        .attr('class', 'states')
        .attr('d', path);

    // Create hexagons of hexbins
    this.svg.append('g')
        .attr('class', 'hexagons')
      .selectAll('path')
        .data(self.hexbin(self.locations).sort(function(a, b) { return b.length - a.length; }))
      .enter().append('path')
        .attr('d', function(d: any) { return self.hexbin.hexagon(radius(d.length)); })
        .attr('transform', function(d: any) { return 'translate(' + d.x + ',' + d.y + ')'; })
        .style('fill', function(d: any) { return (self.color as any)(d3.mean(d, (di: any) => parseInt(di.date, 10))); });
  }

  /* Show warning in map (Hannover blinking red) */
  errorOn(): any {
    // if already blinking, skip
    if (this.isBlinking) {
      console.log('already blinking');
      return;
    }

    this.isBlinking = true;
    console.log('preparing to blink...');

    this.errorTimer = Observable.timer(0, 600)
      .take(10);

    this.errorTimer.subscribe(x => {
      if (x % 2 === 0) {
        this.showErrorLocation(this, this.locations.slice(0, 1));
      } else {
        this.hideErrorLocation();
      }

      if (x === 9) {
        console.log('turning off blinking...');
        this.isBlinking = false;
      }
    });
  }

  /* Shows red warning dot */
  showErrorLocation(self, location: any): any {
    // Do not duplicate element
    if (!this.svg.select('#warning_location').empty()) {
      return;
    }

    // Add element to SVG and fade in
    let errLoc = this.svg.append('g')
        .attr('class', 'hexagons')
        .attr('id', 'warning_location')
      .selectAll('path')
        .data(self.hexbin(location).sort(function(a, b) { return b.length - a.length; }))
      .enter().append('path')
        .attr('d', function(d: any) { return self.hexbin.hexagon(12); })
        .attr('transform', function(d: any) { return 'translate(' + d.x + ',' + d.y + ')'; })
        .style('fill', '#f44336');

    errLoc.style('opacity', 0)
           .transition().duration(400).style('opacity', 1);

    return;
  }

  /* Hide red warning dot */
  hideErrorLocation(): any {
    let errLoc = this.svg.select('#warning_location');
    errLoc
      .style('opacity', 1)
      .transition().duration(400).style('opacity', 0)
      .each('end', function() {
         // Remove element from SVG
        errLoc.remove();
      });
  }

}
