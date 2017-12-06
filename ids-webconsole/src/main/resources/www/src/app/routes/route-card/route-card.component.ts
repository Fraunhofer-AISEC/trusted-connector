import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from '../route';
import { RouteService } from '../route.service';

declare var Viz: any;

@Component({
  selector: 'route-card',
  templateUrl: './route-card.component.html',
  styleUrls: ['./route-card.component.css']
})
export class RouteCardComponent implements OnInit {
  @Input() route: Route;
  vizResult: SafeHtml;
  result: string;
  statusIcon: string;
  statusColor: string;
  statusTextColor: string;

  constructor(private dom: DomSanitizer, private routeService: RouteService) {}

  ngOnInit(): void {
    let graph = this.route.dot;
    if(this.route.status == "Started") {
      this.statusIcon = "stop";
      this.statusColor = "";
      this.statusTextColor = "";
    } else {
      this.statusIcon = "play_arrow";
      this.statusColor = "card-dark";
    }

 	this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph, {engine:"dot"}));
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.route.status = 'Started';
     this.statusIcon = "play_arrow";
     this.statusColor = "";
     this.statusTextColor = "";
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.route.status = 'Stopped';
     this.statusIcon = "stop";
     this.statusColor = "card-dark";
  }

  onToggle(routeId: string): boolean {
    if(this.statusIcon == "play_arrow") {
      this.statusIcon = "stop";
      this.routeService.startRoute(routeId).subscribe(result => {
         this.result = result;
       });
       this.route.status = 'Started';
       this.statusColor = "";
       this.statusTextColor = "";
    } else {
      this.statusIcon = "play_arrow";
      this.routeService.stopRoute(routeId).subscribe(result => {
         this.result = result;
       });

       this.route.status = 'Stopped';
       this.statusColor = "card-dark";
    }
    return true;
  }
}
