import { Routes, RouterModule } from '@angular/router';

import { DashboardComponent } from './dashboard/dashboard.component';
import { AppsComponent } from './apps/apps.component';

import { DataflowpoliciesComponent } from './dataflowpolicies/dataflowpolicies.component';
import { RoutesComponent } from './routes/routes.component';
import { RouteeditorComponent } from './routes/routeeditor/routeeditor.component';
import { IdsComponent } from './ids/ids.component';
import { KeycertsComponent } from './keycerts/keycerts.component';

import { DataFlowComponent } from './dataFlow/dataFlow.component';

const appRoutes: Routes = [{
    path: '',
    redirectTo: '/dashboard',
  	pathMatch: 'full'
  }, {
    path: 'dashboard',
    component: DashboardComponent,
    data: {
      title: 'Dashboard'
    }
  }, {
    path: 'apps',
    component: AppsComponent
  }, {
    path: 'dataflowpolicies',
    component: DataflowpoliciesComponent
  }, {
    path: 'dataflow',
    component: DataFlowComponent
  }, {
    path: 'routes',
    component: RoutesComponent,
  }, {
    path: 'routeeditor',
    component: RouteeditorComponent
  }, {
    path: 'ids',
    component: IdsComponent,
    canDeactivate: [IdsComponent],
  }, {
    path: 'certificates',
    component: KeycertsComponent
  },
];

export const routing = RouterModule.forRoot(appRoutes, { useHash: true });
