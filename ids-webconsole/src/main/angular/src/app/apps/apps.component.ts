import { AfterViewInit, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { App } from './app';
import { AppService } from './app.service';

declare let componentHandler: any;

@Component({
  templateUrl: './apps.component.html',
  providers: []
})
export class AppsComponent implements AfterViewInit {
  public apps: App[];

  constructor(private readonly appService: AppService, private readonly titleService: Title) {
    this.titleService.setTitle('Apps');

    this.appService.getApps()
      .subscribe(apps => {
        this.apps = apps;
      });
  }

  public ngAfterViewInit(): void {
        componentHandler.upgradeAllRegistered();
   }

  public trackApps(index: number, item: App): string {
    return item.id;
  }
}
