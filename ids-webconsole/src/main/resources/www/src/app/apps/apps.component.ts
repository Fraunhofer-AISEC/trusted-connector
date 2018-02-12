import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { App } from './app';
import { AppService } from './app.service';
import { AppCardComponent } from './app-card.component';

@Component({
  templateUrl: './apps.component.html',
  providers: []
})
export class AppsComponent {
  apps: Array<App>;

  constructor(private appService: AppService, private titleService: Title) {
    this.titleService.setTitle('Apps');

    this.appService.getApps()
      .subscribe(apps => {
        this.apps = apps;
      });
  }

  trackApps(index: number, item: App): string {
    return item.id;
  }
}
