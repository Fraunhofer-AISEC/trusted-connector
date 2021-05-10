import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { App } from '../apps/app';
import { AppService } from '../apps/app.service';
import { PolicyService } from '../dataflowpolicies/policy.service';
import { MetricService } from '../metric/metric.service';
import { RouteComponent } from '../routes/route';
import { RouteService } from '../routes/route.service';

@Component({
  templateUrl: './dashboard.component.html',
  providers: []
})
export class DashboardComponent implements OnInit {
  @Output() public readonly changeTitle = new EventEmitter();
  public camelComponents: RouteComponent[];
  public apps: App[];
  public cmlVersion: string;
  public policies = 0;
  public metric: string[] = [];

  constructor(private readonly titleService: Title, private readonly appService: AppService, private readonly routeService: RouteService,
              private readonly policyService: PolicyService, private readonly metricService: MetricService) {
    this.titleService.setTitle('Overview');

    this.appService.getApps()
      .subscribe(apps => {
        this.apps = apps;
      });
  }

  public ngOnInit(): void {
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

  public trackComponents(index: number, item: RouteComponent): string {
    return item.bundle;
  }

}
