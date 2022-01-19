import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule, Title } from '@angular/platform-browser';
import 'material-design-lite';
import { AceEditorModule } from 'ng2-ace-editor';

import { AuthGuard } from './_guards/auth.guard';
import { JwtInterceptor } from './_interceptors/jwt.interceptor';
import { AppComponent } from './app.component';
import { routing } from './app.routing';
import { HTTP_PROVIDER } from './application-http-client.service';
import { AppCardComponent } from './apps/app-card.component';
import { AppSearchResultCardComponent } from './apps/app-search-result-card.component';
import { AppService } from './apps/app.service';
import { AppsComponent } from './apps/apps.component';
import { AppsSearchComponent } from './apps/apps-search.component';
import { ConfirmComponent } from './confirm/confirm.component';
import { ConfirmService } from './confirm/confirm.service';
import { ConnectionConfigurationComponent } from './connection-configuration/connection-configuration.component';
import { ConnectionConfigurationService } from './connection-configuration/connection-configuration.service';
import { ConnectionReportComponent } from './connection-report/connection-report.component';
import { ConnectionReportService } from './connection-report/connection-report.service';
import { DashboardComponent } from './dashboard/dashboard.component';
import { MetricCardComponent } from './dashboard/metric-card.component';
import { DataflowPoliciesComponent } from './dataflowpolicies/dataflowpolicies.component';
import { NewDataflowPolicyComponent } from './dataflowpolicies/dataflowpoliciesnew.component';
import { PolicyService } from './dataflowpolicies/policy.service';
import { IdsComponent } from './ids/ids.component';
import { SettingsService } from './ids/settings.service';
import { CertUploadComponent } from './keycerts/cert-upload.component';
import { CertificateCardComponent } from './keycerts/certificate-card.component';
import { NewIdentityComponent } from './keycerts/identitynew.component';
import { CertificateService } from './keycerts/keycert.service';
import { KeycertsComponent } from './keycerts/keycerts.component';
import { HomeLayoutComponent } from './layouts/home-layout/home-layout.component';
import { LoginLayoutComponent } from './layouts/login-layout/login-layout.component';
import { LoginComponent } from './login/login.component';
import { LoginService } from './login/login.service';
import { MDLUpgradeElementDirective } from './mdl-upgrade-element-directive';
import { MetricService } from './metric/metric.service';
import { PrettifyPipe } from './prettify-json.pipe';
import { RouteCardComponent } from './routes/route-card/route-card.component';
import { RouteService } from './routes/route.service';
import { RouteeditorComponent } from './routes/routeeditor/routeeditor.component';
import { RoutesComponent } from './routes/routes.component';
import { ZoomVizComponent } from './routes/zoom-viz/zoom-viz.component';
import { SensorService } from './sensor/sensor.service';
import { ValuesPipe } from './values.pipe';
import { UsersComponent } from './users/users.component';
import { NewUserComponent } from './users/usernew.component';
import { DetailUserComponent } from './users/userdetail.component';
import { UserService } from './users/user.service';
import { UserCardComponent } from './users/user-card.component';

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
    MetricCardComponent,
    AppsComponent,
    AppCardComponent,
    AppSearchResultCardComponent,
    LoginComponent,
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
    MDLUpgradeElementDirective,
    HomeLayoutComponent,
    LoginLayoutComponent,
    NewUserComponent,
    DetailUserComponent,
    UserCardComponent,
    UsersComponent
  ],
  providers: [
    HTTP_PROVIDER,
    AppService,
    RouteService,
    PolicyService,
    SettingsService,
    CertificateService,
    LoginService,
    SensorService,
    ConfirmService,
    IdsComponent,
    ConnectionReportService,
    ConnectionConfigurationService,
    MetricService,
    Title,
    AuthGuard,
    {
            provide: HTTP_INTERCEPTORS,
            useClass: JwtInterceptor,
            multi: true
    },
    UserService
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
