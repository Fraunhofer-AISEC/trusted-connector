import { Component, OnInit, ViewContainerRef, EventEmitter, Output,  AfterViewInit, ViewEncapsulation } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/timer';

import * as d3 from 'd3';
import * as topojson from 'topojson';
import d3_hexbin from 'd3-plugins-dist/dist/mbostock/hexbin/amd';

@Component({
  selector: 'network-graph',
  template: `<div id="network-graph"></div>`,
  styleUrls:  ['./network-graph.component.css'],  // Include CSS classes for land and countries
  encapsulation: ViewEncapsulation.None   // Generate global CSS classes
})
export class NetworkGraphComponent implements OnInit, AfterViewInit {
  private chart: any;

  constructor() {
  }

  ngOnInit(): void {
  }

  loadMap(): Promise<any> {
    return new Promise((resolve, reject) => {
      d3.json('data/eu.topo.json', (error, json) => {
        if(error) {
          return reject(error);
        }

        resolve(json);
      });
    });
  }

  loadLocations(): Promise<any[]> {
    return new Promise((resolve, reject) => {
      d3.tsv('data/locations.tsv', (error, json) => {
        if(error) {
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

    let color = d3.time.scale<string>()
        .domain([2000, 100000])
        .range(['#004D40', '#4DB6AC'])
        .interpolate(d3.interpolateLab);

    let hexbin = d3_hexbin.default()
        .size([width, height])
        .radius(10);

    let radius = d3.scale.sqrt()
        .domain([0, 12])
        .range([0, 10]);

    let projection = d3.geo.mercator()
        .scale(1720)
        .translate([width-1000, height+1600])
        .precision(.1);

    let path = d3.geo.path()
        .projection(projection);

    let svg = d3.select('#network-graph')
        .append('svg')
        .attr('width', width)
        .attr('height', height);

    let map = await this.loadMap();
    let locations = await this.loadLocations();

    locations.forEach(function(d) {
      let p = projection(d);
      d[0] = p[0], d[1] = p[1];
    });

    // Draw Europe
    svg.append('path')
        .datum(topojson.feature(map, map.objects.europe))
        .attr('class', 'land')
        .attr('d', path);

    // Draw countries
    svg.append('path')
        .datum(topojson.mesh(map, map.objects.europe, function(a, b) { return a !== b; }))
        .attr('class', 'states')
        .attr('d', path);

    // Create hexagons of hexbins
    svg.append('g')
        .attr('class', 'hexagons')
      .selectAll('path')
        .data(hexbin(locations).sort(function(a, b) { return b.length - a.length; }))
      .enter().append('path')
        .attr('d', function(d: any) { return hexbin.hexagon(radius(d.length)); })
        .attr('transform', function(d: any) { return 'translate(' + d.x + ',' + d.y + ')'; })
        .style('fill', function(d: any) { return (color as any)(d3.mean(d, (d: any) => { return +parseInt(d.date); })); });

  }

}
