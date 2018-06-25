import { NgModule } from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { AceEditorModule } from 'ng2-ace-editor';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';

import { DashboardComponent } from './dashboard/dashboard.component';
import { NetworkGraphComponent } from './dashboard/network-graph.component';
import { MetricCardComponent } from './dashboard/metric-card.component';

import { AppsComponent, AppsSearchComponent } from './apps/apps.component';
import { AppCardComponent } from './apps/app-card.component';
import { AppSearchResultCardComponent } from './apps/app-search-result-card.component';

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

import { ConnectionReportService } from './connectionReport/connectionReport.service';
import { ValuesPipe } from './values.pipe';

import { routing } from './app.routing';

import { PrettifyPipe } from './prettify-json.pipe';

import { ConfirmService } from './confirm/confirm.service';
import { ConfirmComponent } from './confirm/confirm.component';

import { ConnectionReportComponent } from './connectionReport/connectionReport.component';
import { ConnectionConfigurationComponent } from './connectionConfiguration/connection-configuration.component';
import { ConnectionConfigurationService } from './connectionConfiguration/connection-configuration.service';

import { HTTP_PROVIDER } from './application-http-client.service';

import 'material-design-lite';
import { MetricService } from './metric/metric.service';

import { ZoomVizComponent } from './routes/zoom-viz/zoom-viz.component';

import { MDLUpgradeElementDirective } from './mdl-upgrade-element-directive';

// import { D3Service } from 'd3-ng2-service';
// import { D3Component } from './d3/d3.component';

@NgModule({
  imports: [
    AceEditorModule,
    BrowserModule,
    routing,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    FormsModule
//    ModalModule.forRoot(),
//    BootstrapModalModule
  ],
  declarations: [
    AppComponent,
    AppsSearchComponent,
    DashboardComponent,
    NetworkGraphComponent,
    MetricCardComponent,
    AppsComponent,
    AppCardComponent,
    AppSearchResultCardComponent,
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
    ConnectionReportComponent,
    ZoomVizComponent,
    ConnectionReportComponent,
    MDLUpgradeElementDirective
    // D3Component,
  ],
  providers: [
    HTTP_PROVIDER,
    AppService,
    RouteService,
    PolicyService,
    SettingsService,
    CertificateService,
    SensorService,
    ConfirmService,
    IdsComponent,
    ConnectionReportService,
    ConnectionConfigurationService,
    MetricService,
    Title
    // D3Service,
  ],
  bootstrap: [
    AppComponent
  ],
  entryComponents: []
})
export class AppModule { }
