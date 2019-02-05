import { RouterModule, Routes } from '@angular/router';

import { AppsComponent, AppsSearchComponent } from './apps/apps.component';
import { ConnectionConfigurationComponent } from './connection-configuration/connection-configuration.component';
import { ConnectionReportComponent } from './connection-report/connection-report.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { DataflowPoliciesComponent } from './dataflowpolicies/dataflowpolicies.component';
import { NewDataflowPolicyComponent } from './dataflowpolicies/dataflowpoliciesnew.component';
import { IdsComponent } from './ids/ids.component';
import { NewIdentityComponent } from './keycerts/identitynew.component';
import { KeycertsComponent } from './keycerts/keycerts.component';
import { RouteeditorComponent } from './routes/routeeditor/routeeditor.component';
import { RoutesComponent } from './routes/routes.component';

const appRoutes: Routes = [{
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },                       {
    path: 'dashboard',
    component: DashboardComponent,
    data: {
      title: 'Dashboard'
    }
  },                       {
    path: 'apps',
    component: AppsComponent
  },                       {
      path: 'appsearch',
      component: AppsSearchComponent
  },                       {
    path: 'dataflowpolicies',
    component: DataflowPoliciesComponent
  },                       {
    path: 'dataflowpolicyynew',
    component: NewDataflowPolicyComponent
  },                       {
    path: 'identitynew',
    component: NewIdentityComponent
  },                       {
    path: 'connections',
    component: ConnectionReportComponent
  },                       {
    path: 'connectionconfiguration',
    component: ConnectionConfigurationComponent
  },                       {
    path: 'routes',
    component: RoutesComponent
  },                       {
    path: 'routeeditor/:id',
    component: RouteeditorComponent
  },                       {
    path: 'routeeditor',
    component: RouteeditorComponent
  },                       {
    path: 'ids',
    component: IdsComponent,
    canDeactivate: [IdsComponent]
  },                       {
    path: 'certificates',
    component: KeycertsComponent
  }
];

export const routing = RouterModule.forRoot(appRoutes, { useHash: true });
