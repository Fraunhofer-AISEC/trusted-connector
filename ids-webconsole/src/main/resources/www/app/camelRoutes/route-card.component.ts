import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from './route';
import { CamelRoutesService } from './camelRoutes.service';

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
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i>start</i></a>
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect" (click)="onStop(route.id)"><i class="material-icons" role="presentation">pause</i></a>
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a>
      </div>`
})
export class RouteCardComponent implements OnInit {
  @Input() route: Route;
  vizResult: SafeHtml;
  result: string;

  constructor(private dom: DomSanitizer, private camelRoutesService: CamelRoutesService) {}

  ngOnInit(): void {
    var graph = this.route.dot;
    this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph));
  }

  onStop(routeId: string): void {
      this.camelRoutesService.stopRoute(routeId).subscribe(result => {
         this.result = result;
       });
       this.route.status = "Stopped";
  }
}
