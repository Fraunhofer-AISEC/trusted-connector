import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from './route';
import { RouteService } from './route.service';

declare var Viz: any;

@Component({
  selector: 'route-card',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{route.id}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        {{route.description}}
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--2-col">Uptime</div><div class="mdl-cell mdl-cell--10-col">{{(route.uptime/1000/60).toFixed()}} minutes</div>
          <div class="mdl-cell mdl-cell--2-col">Context</div><div class="mdl-cell mdl-cell--10-col">{{route.context}}</div>
          <div class="mdl-cell mdl-cell--2-col">Status</div><div class="mdl-cell mdl-cell--10-col">{{route.status}}</div>
        </div>
        <div style="padding-top:30px" [innerHTML]="vizResult"></div>
      </div>
      <div class="mdl-card__actions mdl-card--border">
          <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab mdl-button--colored" (click)="onToggle(route.id)">
            <i class="material-icons">{{statusIcon}}</i>
          </button>
          <!--<a class="mdl-button mdl-js-button mdl-js-ripple-effect" (click)="onToggle(route.id)"><i class="material-icons" role="presentation">{{statusIcon}}</i></a>
          <a class="mdl-button mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a> -->
          <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab mdl-button--colored">
            <i class="material-icons">delete</i>
          </button>

      </div>`
})
export class RouteCardComponent implements OnInit {
  @Input() route: Route;
  vizResult: SafeHtml;
  result: string;
  statusIcon: string;


  constructor(private dom: DomSanitizer, private routeService: RouteService) {}

  ngOnInit(): void {
    let graph = this.route.dot;
    if(this.route.status == "Started") {
      this.statusIcon = "play_arrow";
    } else {
      this.statusIcon = "stop";

    }

  //this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph));
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.route.status = 'Started';
       this.statusIcon = "play_arrow";
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.route.status = 'Stopped';
     this.statusIcon = "stop";
  }

  onToggle(routeId: string): void {
    if(this.statusIcon == "play_arrow") {
      this.statusIcon = "stop";
      this.routeService.stopRoute(routeId).subscribe(result => {
         this.result = result;
       });
       this.route.status = 'Stopped';

    } else {
      this.statusIcon = "play_arrow";
      this.routeService.startRoute(routeId).subscribe(result => {
         this.result = result;
       });
       this.route.status = 'Started';
    }
  }
}
