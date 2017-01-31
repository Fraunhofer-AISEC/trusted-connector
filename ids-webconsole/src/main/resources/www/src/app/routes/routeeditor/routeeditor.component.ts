import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from '../route';
import { RouteService } from '../route.service';

import { ActivatedRoute } from '@angular/router';
import 'rxjs/add/operator/switchMap';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{camelRoute.id}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        {{camelRoute.description}}
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--2-col">Uptime</div><div class="mdl-cell mdl-cell--10-col">{{(camelRoute.uptime/1000/60).toFixed()}} minutes</div>
          <div class="mdl-cell mdl-cell--2-col">Context</div><div class="mdl-cell mdl-cell--10-col">{{camelRoute.context}}</div>
          <div class="mdl-cell mdl-cell--2-col">Status</div><div class="mdl-cell mdl-cell--10-col">{{camelRoute.status}}</div>
        </div>
        <div style="padding-top:30px" [innerHTML]="vizResult"></div>
      </div>
      <div class="mdl-card__actions mdl-card--border">
          <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab" (click)="onToggle(camelRoute.id)">
            <i class="material-icons">{{statusIcon}}</i>
          </button>
          <!--<a class="mdl-button mdl-js-button mdl-js-ripple-effect" (click)="onToggle(camelRoute.id)"><i class="material-icons" role="presentation">{{statusIcon}}</i></a>
          <a class="mdl-button mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a> -->
          <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab">
            <i class="material-icons">delete</i>
          </button>

      </div>`
})
export class RouteeditorComponent implements OnInit {
  private camelRoute: Route = new Route();
  vizResult: SafeHtml;
  statusIcon: string;
  result: string;
  private id: any;  // Camel Route Id
  
  constructor(private navRoute: ActivatedRoute, private dom: DomSanitizer, private routeService: RouteService) {  }

  ngOnInit(): void {
    // Load route parameter. This is done by an observable because router may not recreate this component
    console.log(this.navRoute.snapshot.params['id']);
    this.routeService
        .getRoutes()
        .subscribe(camelRoute => {
          console.log("Received a route " + camelRoute[0].id);
          this.camelRoute = camelRoute[0];
          let graph = this.camelRoute.dot;

          if(this.camelRoute.status == "Started") {
            this.statusIcon = "stop";
          } else {
            this.statusIcon = "play_arrow";

          }

          this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph));
        });
  }
    

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.camelRoute.status = 'Started';
       this.statusIcon = "play_arrow";
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.camelRoute.status = 'Stopped';
     this.statusIcon = "stop";
  }

  onToggle(routeId: string): void {
    if(this.statusIcon == "play_arrow") {
      this.statusIcon = "stop";
      this.routeService.startRoute(routeId).subscribe(result => {
         this.result = result;
       });
       this.camelRoute.status = 'Started';

    } else {
      this.statusIcon = "play_arrow";
      this.routeService.stopRoute(routeId).subscribe(result => {
         this.result = result;
       });

       this.camelRoute.status = 'Stopped';
    }
  }
}
