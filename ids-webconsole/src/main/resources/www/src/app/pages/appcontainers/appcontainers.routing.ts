import { Routes, RouterModule }  from '@angular/router';

import { AppContainer } from './appcontainers.component';
import { AppContainerTable } from './components/appcontainerTable/appcontainerTable.component';

// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: '',
    component: AppContainerTable,
    children: [
      //{ path: 'appcontainers', component: AppContainerTable }
    ]
  }
];

export const routing = RouterModule.forChild(routes);
