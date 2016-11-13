import { Routes, RouterModule }  from '@angular/router';

import { AppContainer } from './appcontainers.component';
import { AppContainerMasonry } from './components/appcontainerMasonry/appcontainerMasonry.component';

// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: '',
    component: AppContainerMasonry,
    children: [
      //{ path: 'appcontainers', component: AppContainerTable }
    ]
  }
];

export const routing = RouterModule.forChild(routes);
