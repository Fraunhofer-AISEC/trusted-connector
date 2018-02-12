import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { App } from './app';
import { AppService } from './app.service';

@Component({
    selector: 'app-card',
    template: `
  <div class="app-card  {{statusColor}} mdl-card mdl-cell mdl-cell--12-col mdl-shadow--2dp">
    <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{app.names}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        <div class="mdl-grid">
          <div class="mdl-cell mdl-cell--4-col bold">
            Trust
          </div>
          <div class="mdl-cell mdl-cell--4-col bold">
            Uptime
          </div>
          <div class="mdl-cell mdl-cell--4-col bold">
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
          <button class="mdl-button mdl-js-button mdl-js-ripple-effect" (click)="onToggle(app.id)">
            <i class="material-icons">{{statusIcon}}</i>
          </button>
          <button class="mdl-button mdl-js-button mdl-js-ripple-effect">
            <i class="material-icons">delete</i>
          </button>
  </div>
 </div>`
})
export class AppCardComponent implements OnInit {
    @Input() app: App;
    statusIcon: string;
    statusColor: string;

    constructor(private appService: AppService) { }
    ngOnInit(): void {
        if (this.app.status.indexOf('Up') >= 0) {
            this.statusIcon = 'stop';
            this.statusColor = '';
        } else {
            this.statusIcon = 'play_arrow';
            this.statusColor = 'card-dark';
        }
    }

    onToggle(containerId: string): void {
        if (this.statusIcon === 'play_arrow') {
            this.statusIcon = 'stop';
            this.statusColor = '';
            this.appService.startApp(containerId).subscribe(result => {});
            this.app.status = 'Up 1 seconds ago';

        } else {
            this.statusIcon = 'play_arrow';
            this.statusColor = 'card-dark';
            this.appService.stopApp(containerId).subscribe(result => {});
            this.app.status = 'Exited(0) 1 seconds ago';
        }
    }
}
