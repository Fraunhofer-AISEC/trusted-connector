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
  private camelComponents: any;
  apps: App[];
  messages: number = 0;

  constructor(private titleService: Title, private appService: AppService, private routeService: RouteService) {
  	 super();
     this.titleService.setTitle('Overview');

    this.appService.getApps().subscribe(apps => {
      this.apps = apps;
    });
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
    this.routeService.listComponents().subscribe(result => {this.camelComponents = result});
  }

}
