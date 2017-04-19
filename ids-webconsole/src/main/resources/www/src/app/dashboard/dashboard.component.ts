import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { RouteService } from '../routes/route.service';
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

  constructor(private titleService: Title, private routeService: RouteService) {
     this.titleService.setTitle('Dashboard');
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
    this.routeService.listComponents().subscribe(result => {this.camelComponents = result});
    console.log(this.camelComponents);
  }

}
