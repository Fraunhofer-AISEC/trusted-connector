import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { SubscriptionComponent } from '../subscription.component';

import { Observable } from 'rxjs/Observable';

import { App } from '../apps/app';
import { AppService } from '../apps/app.service';
import { RouteService } from '../routes/route.service';

@Component({
  templateUrl: './dashboard.component.html',
  providers: []
})
export class DashboardComponent extends SubscriptionComponent implements OnInit {
  @Output() changeTitle = new EventEmitter();

  apps: App[];

  messages: number = 0;

  isRouteActive: boolean = false;

  private lastEvent: Date;

  constructor(private titleService: Title, private appService: AppService, private routeService: RouteService) {
    super();

    this.appService.getApps().subscribe(apps => {
      this.apps = apps;
    });

    this.subscriptions.push(
      Observable
        .timer(0, 1000)
        .flatMap(() => { return this.routeService.getRoutes(); })
        .subscribe(routes => {
          this.isRouteActive = false;
          
          routes.forEach((route) => {
            this.messages += +route.messages;

            if(route.id == "OPC-UA: Read Engine RPM (Trusted)" && route.status == "Started") {
              this.isRouteActive = true;
            }
          });
        }));
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
  }

}
