import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import {  SubscriptionComponent } from '../subscription.component';

import { Observable } from 'rxjs/Observable';

import { App } from '../apps/app';
import { AppService } from '../apps/app.service';
import { MetricService } from '../metric/metric.service';
import { RouteService } from '../routes/route.service';
import { PolicyService } from '../dataflowpolicies/policy.service';
import { RouteComponent } from '../routes/route';

@Component({
  templateUrl: './dashboard.component.html',
  providers: []
})
export class DashboardComponent extends SubscriptionComponent implements OnInit {
  @Output() changeTitle = new EventEmitter();
  camelComponents: Array<RouteComponent>;
  apps: Array<App>;
  cmlVersion: string;
  policies = 0;
  metric: Array<String> = [];

  constructor(private titleService: Title, private appService: AppService, private routeService: RouteService,
              private policyService: PolicyService, private metricService: MetricService) {
    super();
    this.titleService.setTitle('Overview');

    this.appService.getApps()
      .subscribe(apps => {
        this.apps = apps;
      });
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
    this.routeService.listComponents()
      .subscribe(result => { this.camelComponents = result; });
    this.appService.getCmlVersion()
      .subscribe(result => { this.cmlVersion = result.cml_version; });
    this.policyService.getPolicies()
      .subscribe(result => { this.policies = result.length; });
    this.metricService.getMetricObservable()
      .subscribe(result => { this.metric = result; });
  }

  trackComponents(index: number, item: RouteComponent): string {
    return item.bundle;
  }

}
