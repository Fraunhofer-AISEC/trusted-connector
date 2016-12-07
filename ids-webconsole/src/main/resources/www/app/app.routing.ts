import { Routes, RouterModule } from '@angular/router';

import { DashboardComponent } from './dashboard/dashboard.component';
import { AppsComponent } from './apps/apps.component';

import { CamelRoutesComponent } from './camelRoutes/camelRoutes.component';
import { IdsComponent } from './ids/ids.component';
import { KeycertsComponent } from './keycerts/keycerts.component';

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
    path: 'routes',
    component: CamelRoutesComponent
  }, {
    path: 'ids',
    component: IdsComponent
  }, {
    path: 'certificates',
    component: KeycertsComponent
  }

];

export const routing = RouterModule.forRoot(appRoutes, { useHash: true });
