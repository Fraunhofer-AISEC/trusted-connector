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

import { DataflowPoliciesComponent } from './dataflowpolicies/dataflowpolicies.component';
import { NewDataflowPolicyComponent } from './dataflowpolicies/dataflowpoliciesnew.component';
import { RoutesComponent } from './routes/routes.component';
import { RouteeditorComponent } from './routes/routeeditor/routeeditor.component';
import { RouteCardComponent } from './routes/route-card.component';

import { IdsComponent } from './ids/ids.component';
import { SettingsService } from './ids/settings.service';


import { KeycertsComponent } from './keycerts/keycerts.component';
import { CertificateCardComponent } from './keycerts/certificate-card.component';
import { CertificateService } from './keycerts/keycert.service';
import { CertUploadComponent } from './keycerts/certUpload.component';
import { NewIdentityComponent } from './keycerts/identitynew.component';


import { AppService } from './apps/app.service';
import { RouteService } from './routes/route.service';
import { SensorService } from './sensor/sensor.service';
import { PolicyService } from './dataflowpolicies/policy.service';
//Do we need this declaration?
import { ConnectionInOutService } from './inOutConnections/inOutConnections.service';
import { ValuesPipe } from './values.pipe';

import { routing } from './app.routing';

import {PrettifyPipe} from './prettify-json.pipe';

import {ConfirmService} from "./confirm/confirm.service";
import {ConfirmComponent} from "./confirm/confirm.component";

import { ConnectionInOutComponent } from './inOutConnections/inOutConnections.component'
import { ConnectionConfigurationComponent } from './connectionConfiguration/connectionConfiguration.component';
import { ConnectionConfigurationService } from './connectionConfiguration/connectionConfiguration.service';


import { MDLTextFieldDirective } from './mdl-textfield-directive';

import 'material-design-lite';
import { MetricService } from './metric/metric.service';

@NgModule({
  imports: [
    BrowserModule,
    routing,
    HttpModule,
    FormsModule,
    ReactiveFormsModule
    ],
  declarations: [
    AppComponent,
    DashboardComponent,
    ActivityComponent,
    NetworkGraphComponent,
    MetricCardComponent,
    AppsComponent,
    AppCardComponent,
    DataflowPoliciesComponent,
    NewDataflowPolicyComponent,
    RoutesComponent,
    RouteeditorComponent,
    RouteCardComponent,
    IdsComponent,
    KeycertsComponent,
    CertificateCardComponent,
    CertUploadComponent,
    NewIdentityComponent,
    ConfirmComponent,
    ValuesPipe,
    PrettifyPipe,
    ConnectionConfigurationComponent,
    MDLTextFieldDirective,
    ConnectionInOutComponent
  ],
  providers: [
    AppService,
    RouteService,
    PolicyService,
    SettingsService,
    CertificateService,
    SensorService,
    ConfirmService,
    IdsComponent,
    ConnectionInOutService,
    ConnectionConfigurationService,
    MetricService,
    Title
  ],
  bootstrap: [
    AppComponent ],
    entryComponents: [
      ]
})
export class AppModule { }
