import { Component, AfterViewInit, ElementRef, ViewChild, Input } from '@angular/core';
import { D3Service, D3, Selection } from 'd3-ng2-service';
import { SimulationLinkDatum, SimulationNodeDatum } from 'd3-force';

import { NodeData, Edge, GraphData } from './graph';
import { Route } from '../routes/route';
import { Subject } from 'rxjs/Subject';

@Component({
  selector: 'app-d3',
  templateUrl: './d3.component.html',
  styleUrls: ['./d3.component.css']
})
export class D3Component implements AfterViewInit {

  @ViewChild('d3Pane') private d3Pane: ElementRef;
  @Input() private routeSubject: Subject<Route>;

  constructor(private d3Service: D3Service) {}

  ngAfterViewInit() {
    this.routeSubject.subscribe(route => {
      let graph = route.graph;
      let d3 = this.d3Service.getD3();
      let color = d3.scaleOrdinal(d3.schemeCategory20);
      let bbox = this.d3Pane.nativeElement.getBoundingClientRect();
      let width = parseInt(bbox.width), height = parseInt(bbox.height);
      let svg = d3.select(this.d3Pane.nativeElement);
      
      let simulation = d3.forceSimulation().nodes(graph.nodes)
        .force("link", d3.forceLink(graph.links).id((d: NodeData) => { return d.name; }))
        .force("charge", d3.forceManyBody().strength(-100))
        .force("center", d3.forceCenter(width * 0.1, height / 2))
        .force("x", d3.forceX(width * 0.1).strength(0.5))
        .force("collide", d3.forceCollide());
  
      let painted = false;
  
      simulation.on("end", () => {
        if (painted) {
          return;
        } else {
          painted = true;
        }
  
        let links = svg.append("g")
          .attr("class", "links")
          .selectAll("line")
          .data(graph.links)
          .enter().append("line")
            .attr("stroke-width", 1);
      
        let nodes = svg.append("g")
          .attr("class", "nodes")
          .selectAll("circle")
          .data(graph.nodes)
          .enter()
            .append("g")
              .attr("class", "node");
  
        nodes.each(function (d: NodeData) { 
          d3.select(this)
            .append("circle")
              .attr("r", 5)
              .attr("fill", color("1"));
          d3.select(this)
            .append("text")
              .attr("dx", 12)
              .attr("dy", ".35em")
              .text(d.action);
        });
  
        d3.selectAll("g.node").call(d3.drag()
          .on("start", (d: SimulationNodeDatum) => {
            if (!d3.event.active) {
              simulation.alphaTarget(0.3).restart();
            }
            d.fx = d.x;
            d.fy = d.y;
          }).on("drag", (d: SimulationNodeDatum) => {
            d.fx = d3.event.x;
            d.fy = d3.event.y;
          }).on("end", (d: SimulationNodeDatum) => {
            if (!d3.event.active) {
              simulation.alphaTarget(0);
            }
            d.fx = null;
            d.fy = null;
          }));
      
        nodes.append("title")
            .text((d: NodeData) => { return d.action; });
      
        simulation.on("tick", () => {
          links
            .attr("x1", (d: SimulationLinkDatum<SimulationNodeDatum>) => {
              return (<SimulationNodeDatum>d.source).x;
            })
            .attr("y1", (d: SimulationLinkDatum<SimulationNodeDatum>) => {
              return (<SimulationNodeDatum>d.source).y;
            })
            .attr("x2", (d: SimulationLinkDatum<SimulationNodeDatum>) => {
              return (<SimulationNodeDatum>d.target).x;
            })
            .attr("y2", (d: SimulationLinkDatum<SimulationNodeDatum>) => {
              return (<SimulationNodeDatum>d.target).y;
            });
          nodes
            .attr("transform", (d: NodeData) => {
              return "translate(" + d.x + "," + d.y + ")";
            });
        });
  
        simulation.alphaTarget(0).restart();
      });
    });
  }

}
