import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { App } from './app';
import { AppService } from './app.service';
import { AppCardComponent } from './app-card.component';

@Component({
  templateUrl: 'app/apps/apps.component.html',
  providers: []
})
export class AppsComponent {
  apps: App[];

  constructor(private appService: AppService, private titleService: Title) {
    this.titleService.setTitle('Apps');

    this.appService.getApps().subscribe(apps => {
       this.apps = apps;
     });
  }
}
