import { NgModule }      from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import { HttpModule }     from '@angular/http';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';

import { DashboardComponent }  from './dashboard/dashboard.component';
import { ActivityComponent }  from './dashboard/activity.component';
import { NetworkGraphComponent }  from './dashboard/network-graph.component';
import { MetricCardComponent }  from './dashboard/metric-card.component';

import { AppsComponent } from './apps/apps.component';
import { AppCardComponent } from './apps/app-card.component';

import { DataflowpoliciesComponent } from './dataflowpolicies/dataflowpolicies.component';
import { RoutesComponent } from './routes/routes.component';
import { RouteeditorComponent } from './routes/routeeditor/routeeditor.component';
import { RouteCardComponent } from './routes/route-card.component';

import { IdsComponent } from './ids/ids.component';
import { SettingsService } from './ids/settings.service';


import { KeycertsComponent } from './keycerts/keycerts.component';
import { CertificateCardComponent } from './keycerts/certificate-card.component';
import { CertificateService } from './keycerts/keycert.service';
import { FileWindow } from './keycerts/uploadCert';

import { AppService } from './apps/app.service';
import { RouteService } from './routes/route.service';

import { ValuesPipe } from './values.pipe';

import { routing } from './app.routing';

import {PrettifyPipe} from './prettify-json.pipe';

import {ConfirmService} from "./confirm/confirm.service";
import {ConfirmComponent} from "./confirm/confirm.component";

import { ModalModule } from 'angular2-modal';
import { BootstrapModalModule } from 'angular2-modal/plugins/bootstrap';


import 'material-design-lite';

@NgModule({
  imports: [
    BrowserModule,
    routing,
    HttpModule,
    ReactiveFormsModule,
    ModalModule.forRoot(),
    BootstrapModalModule  ],
  declarations: [
    AppComponent,
    DashboardComponent,
    ActivityComponent,
    NetworkGraphComponent,
    MetricCardComponent,
    AppsComponent,
    AppCardComponent,
    DataflowpoliciesComponent,
    RoutesComponent,
    RouteeditorComponent,
    RouteCardComponent,
    IdsComponent,
    KeycertsComponent,
    CertificateCardComponent,
    ConfirmComponent,
    ValuesPipe,
    PrettifyPipe,
    FileWindow],
  providers: [
    AppService,
    RouteService,
    SettingsService,
    CertificateService,
    ConfirmService,
    Title
  ],
  bootstrap: [
    AppComponent ],
    entryComponents: [
      FileWindow]
})
export class AppModule { }
