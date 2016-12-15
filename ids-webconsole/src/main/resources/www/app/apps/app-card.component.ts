import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { App } from './app';
import { AppService } from './app.service';

@Component({
  selector: 'app-card',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{app.names}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        Image: {{app.image}}
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--4-col">
            Trust
          </div>
          <div class="mdl-cell mdl-cell--4-col">
            Uptime
          </div>
          <div class="mdl-cell mdl-cell--4-col">
            Ports
          </div>
          <div class="mdl-cell mdl-cell--4-col">
            <div style="color: #209e91">Trusted</div>
          </div>
          <div class="mdl-cell mdl-cell--4-col">
            {{app.uptime}}
          </div>
          <div class="mdl-cell mdl-cell--4-col">
            {{app.ports}}
          </div>
        </div>
        Created: {{app.created}}<br />
        Status: {{app.status}}<br />
      </div>
      <div class="mdl-card__actions mdl-card--border">
          <a class="mdl-button mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">start</i></a>
          <a class="mdl-button mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">pause</i></a>
          <a class="mdl-button mdl-js-button mdl-js-ripple-effect"><i class="material-icons" role="presentation">delete</i></a>
      </div>`
})
export class AppCardComponent implements OnInit {
  @Input() app: App;

  ngOnInit(): void {

  }
}
