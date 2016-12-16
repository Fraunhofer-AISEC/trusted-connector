import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from './route';
import { CamelRoutesService } from './camelRoutes.service';

@Component({
  selector: 'route-card',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{route.id}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        <div class="col-3">  Description: </div> <div class="col-9">{{route.description}}</div>
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--4-col">
            Trust
          </div>
        </div>
        <div class="col-3">Uptime:</div> <div class="col-9">{{route.uptime}}</div>
        <div class="col-3">Context:</div> <div class="col-9">{{route.context}}</div>
        <div class="col-3">Visualization:</div> <div class="col-9">{{route.dot}}</div>
        <div class="col-3">Status:</div> <div class="col-9">{{route.status}}</div>
      </div>
      <div class="mdl-card__actions mdl-card--border">
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">start</i></a>
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">pause</i></a>
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a>
      </div>`,
      styleUrls: ['app/camelRoutes/route-card.component.css']
})
export class RouteCardComponent implements OnInit {
  @Input() route: Route;

  ngOnInit(): void {

  }
}
