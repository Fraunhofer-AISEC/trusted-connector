import { Routes, RouterModule }  from '@angular/router';
import { Pages } from './pages.component';
// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: 'pages',
    component: Pages,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadChildren: () => System.import('./dashboard/dashboard.module') },
      { path: 'apps/installed-apps', loadChildren: () => System.import('./appcontainers/appcontainers.module') },
      { path: 'identities/identities-my', loadChildren: () => System.import('./identities/identities.module') },
      { path: 'pipes', loadChildren: () => System.import('./dashboard/dashboard.module') },
      { path: 'settings', loadChildren: () => System.import('./settings/settings.module') },
    ]
  }
];

export const routing = RouterModule.forChild(routes);
