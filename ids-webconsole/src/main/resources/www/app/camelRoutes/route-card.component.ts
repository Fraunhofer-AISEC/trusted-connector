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
        Description: {{route.description}}
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--4-col">
            Trust
          </div>          
        </div>
        Uptime: {{route.uptime}}<br />
        Context: {{route.context}}<br />
        Visualization: {{route.dot}}<br />
        Status: {{route.status}}<br />
      </div>
      <div class="mdl-card__actions mdl-card--border">
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">start</i></a>
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">pause</i></a>
          <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a>
      </div>`
})
export class RouteCardComponent implements OnInit {
  @Input() route: Route;

  ngOnInit(): void {

  }
}
