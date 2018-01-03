import { NgModule }      from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import { HttpModule }     from '@angular/http';
import { HttpClientModule } from "@angular/common/http";
import { AceEditorModule } from 'ng2-ace-editor';

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
import { RouteCardComponent } from './routes/route-card/route-card.component';

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

import { ValuesPipe } from './values.pipe';

import { routing } from './app.routing';

import {PrettifyPipe} from './prettify-json.pipe';

import {ConfirmService} from "./confirm/confirm.service";
import {ConfirmComponent} from "./confirm/confirm.component";

//import { ModalModule } from 'angular2-modal';
//import { BootstrapModalModule } from 'angular2-modal/plugins/bootstrap';

//import { ConnectionReportComponent } from './inOutConnections/inOutConnections.component'
//import { ConnectionService } from './inOutConnections/inOutConnections.service';

import { MDLTextFieldDirective } from './mdl-textfield-directive';
//import { DataFlowComponent } from './dataFlow/dataFlow.component';
import { ConnectionReportComponent } from './connectionsReport/connectionsReport.component'
import { ConnectionService } from './connectionsReport/connectionReport.service';


import 'material-design-lite';
import { MetricService } from './metric/metric.service';

import { ZoomVizComponent } from 'app/routes/zoom-viz/zoom-viz.component';

// import { D3Service } from 'd3-ng2-service';
// import { D3Component } from './d3/d3.component';

@NgModule({
  imports: [
    AceEditorModule,
    BrowserModule,
    routing,
    HttpModule,             // deprecated. To be removed
    HttpClientModule,       // new since Angular 5
    ReactiveFormsModule,
//    ModalModule.forRoot(),
//    BootstrapModalModule
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
    MDLTextFieldDirective,
    //FileWindow,
    //DataFlowComponent,
    ConnectionReportComponent,
    ZoomVizComponent,
    //D3Component,
  ],
  providers: [
    AppService,
    RouteService,
    PolicyService,
    SettingsService,
    CertificateService,
    SensorService,
    ConfirmService,
    ConnectionService,
    IdsComponent,
    Title,
    MetricService,
    Title
    //D3Service,
  ],
  bootstrap: [
    AppComponent
  ],
  entryComponents: []
})
export class AppModule { }
