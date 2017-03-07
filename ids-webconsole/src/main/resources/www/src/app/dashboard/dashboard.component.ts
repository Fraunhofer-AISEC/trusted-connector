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

  messages: Number = 0;

  private lastEvent: Date;

  constructor(private titleService: Title, private appService: AppService, private routeService: RouteService) {
    super();

    this.titleService.setTitle('Dashboard');

    this.appService.getApps().subscribe(apps => {
      this.apps = apps;
    });

    this.subscriptions.push(
      Observable
        .timer(0, 1000)
        .flatMap(() => { return this.routeService.getTotalMessages(); })
        .subscribe(messages => this.messages = messages));
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
  }

}
