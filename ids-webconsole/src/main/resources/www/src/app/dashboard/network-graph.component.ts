import { Component, OnInit, ViewContainerRef, EventEmitter, Output,  AfterViewInit, ViewEncapsulation } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/timer';

import * as d3 from 'd3';
import * as queue from 'd3-queue';
import * as topojson from 'topojson';
import 'd3-plugins-dist';

@Component({
  selector: 'network-graph',
  template: `<div id="network-graph"></div>`,
  //styleUrls:  ['css/network-graph.css'],  // Include CSS classes for land and countries this file is missing!!!
  encapsulation: ViewEncapsulation.None   // Generate global CSS classes
})
export class NetworkGraphComponent implements OnInit, AfterViewInit {
  private subscriptions: Subscription[] = [];

  private chart: any;

  constructor() {
  }

  ngOnInit(): void {
  }

  /*
   * After view has been rendered, add some JavaScript to it. Adapted from http://bl.ocks.org/mbostock/4330486
   */
  ngAfterViewInit() {
            var width = 960,
                  height = 400;

              var color = d3.time.scale<string>()
                  .domain([2000, 100000])
                  .range(["#004D40", "#4DB6AC"])
                  .interpolate(d3.interpolateLab);

              var hexbin = (d3 as any).hexbin()
                  .size([width, height])
                  .radius(10);

              var radius = d3.scale.sqrt()
                  .domain([0, 12])
                  .range([0, 10]);

              var projection = d3.geo.mercator()
                  .scale(1720)
                  .translate([width-1000, height+1600])
                  .precision(.1);

              var path = d3.geo.path()
                  .projection(projection);

              var svg = d3.select("#network-graph")
                  .append("svg")
                  .attr("width", width)
                  .attr("height", height);

              queue.queue()
                  .defer(d3.json, "js/eu.topo.json")  // Map of Europe
                  .defer(d3.tsv, "js/locations.tsv")  // Locations (cities in Germany w/ population)
                  .await(ready);

              function ready(error, us, locations) {
                if (error) throw error;

                locations.forEach(function(d) {
                  var p = projection(d);
                  d[0] = p[0], d[1] = p[1];
                });

                // Draw Europe
                svg.append("path")
                    .datum(topojson.feature(us, us.objects.europe))
                    .attr("class", "land")
                    .attr("d", path);

                // Draw countries
                svg.append("path")
                    .datum(topojson.mesh(us, us.objects.europe, function(a, b) { return a !== b; }))
                    .attr("class", "states")
                    .attr("d", path);

                // Create hexagons of hexbins
                svg.append("g")
                    .attr("class", "hexagons")
                  .selectAll("path")
                    .data(hexbin(locations).sort(function(a, b) { return b.length - a.length; }))
                  .enter().append("path")
                    .attr("d", function(d: any) { return hexbin.hexagon(radius(d.length)); })
                    .attr("transform", function(d: any) { return "translate(" + d.x + "," + d.y + ")"; })
                    .style("fill", function(d: any) { return (color as any)(d3.mean(d, (d: any) => { return +parseInt(d.date); })); });
              }
  }

  ngOnDestroy(): void {
    console.log("Unsubscribing...");
    for(let subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }
}
