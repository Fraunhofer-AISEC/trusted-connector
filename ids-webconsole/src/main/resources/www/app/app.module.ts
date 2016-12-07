import { NgModule }      from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import { HttpModule }     from '@angular/http';

import { AppComponent } from './app.component';

import { DashboardComponent }  from './dashboard/dashboard.component';
import { ActivityComponent }  from './dashboard/activity.component';

import { AppsComponent } from './apps/apps.component';
import { AppCardComponent } from './apps/app-card.component';

import { CamelRoutesComponent } from './camelRoutes/camelRoutes.component';
import { IdsComponent } from './ids/ids.component';
import { KeycertsComponent } from './keycerts/keycerts.component';

import { AppService } from './apps/app.service';

import { ValuesPipe } from './values.pipe';

import { routing } from './app.routing';

@NgModule({
  imports: [
    BrowserModule,
    routing,
    HttpModule ],
  declarations: [
    AppComponent,
    DashboardComponent,
    ActivityComponent,
    AppsComponent,
    AppCardComponent,
    CamelRoutesComponent,
    IdsComponent,
    KeycertsComponent,
    ValuesPipe ],
  providers: [
    AppService,
    Title
  ],
  bootstrap: [
    AppComponent ]
})
export class AppModule { }
