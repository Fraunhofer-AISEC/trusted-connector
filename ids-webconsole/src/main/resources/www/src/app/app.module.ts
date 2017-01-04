import { NgModule }      from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import { HttpModule }     from '@angular/http';

import { AppComponent } from './app.component';

import { DashboardComponent }  from './dashboard/dashboard.component';
import { ActivityComponent }  from './dashboard/activity.component';
import { NetworkGraphComponent }  from './dashboard/network-graph.component';
import { MetricCardComponent }  from './dashboard/metric-card.component';

import { AppsComponent } from './apps/apps.component';
import { AppCardComponent } from './apps/app-card.component';

import { RoutesComponent } from './routes/routes.component';
import { RouteCardComponent } from './routes/route-card.component';

import { IdsComponent } from './ids/ids.component';
import { KeycertsComponent } from './keycerts/keycerts.component';

import { AppService } from './apps/app.service';
import { RouteService } from './routes/route.service';

import { ValuesPipe } from './values.pipe';

import { routing } from './app.routing';

import 'material-design-lite';

@NgModule({
  imports: [
    BrowserModule,
    routing,
    HttpModule ],
  declarations: [
    AppComponent,
    DashboardComponent,
    ActivityComponent,
    NetworkGraphComponent,
    MetricCardComponent,
    AppsComponent,
    AppCardComponent,
    RoutesComponent,
    RouteCardComponent,
    IdsComponent,
    KeycertsComponent,
    ValuesPipe ],
  providers: [
    AppService,
    RouteService,
    Title
  ],
  bootstrap: [
    AppComponent ]
})
export class AppModule { }
