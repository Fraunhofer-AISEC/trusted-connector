import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from './route';
import { RouteService } from './route.service';

declare var Viz: any;

@Component({
  selector: 'route-card',
  template: `
    <div class="app-card mdl-card mdl-cell {{statusColor}} mdl-cell--12-col  mdl-shadow--2dp">
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{route.id}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        {{route.description}}
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--2-col">Uptime</div><div class="mdl-cell mdl-cell--10-col">{{(route.uptime/1000/60).toFixed()}} minutes</div>
          <div class="mdl-cell mdl-cell--2-col">Context</div><div class="mdl-cell mdl-cell--10-col">{{route.context}}</div>
          <div class="mdl-cell mdl-cell--2-col">Status</div><div class="mdl-cell mdl-cell--10-col">{{route.status}}</div>
	      <div class="mdl-cell mdl-cell--12-col" style="padding-top:30px" [innerHTML]="vizResult"></div>
        </div>
      </div>
      <div class="mdl-card__actions mdl-card--border">
          <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab" (click)="onToggle(route.id)">
            <i class="material-icons">{{statusIcon}}</i>
          </button>
          <!--<a class="mdl-button mdl-js-button mdl-js-ripple-effect" (click)="onToggle(route.id)"><i class="material-icons" role="presentation">{{statusIcon}}</i></a>
          <a class="mdl-button mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a> -->
          <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab" disabled>
            <i class="material-icons">delete</i>
          </button>
          <a class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab" routerLink="/routeeditor">
            <i class="material-icons">edit</i>
          </a>

    </div>
   </div>`
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

  onToggle(routeId: string): void {
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
  }
}
