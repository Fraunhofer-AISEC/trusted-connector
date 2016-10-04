import { Routes, RouterModule }  from '@angular/router';

import { Settings } from './settings.component';

// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: '',
    component: Settings
  }
];

export const routing = RouterModule.forChild(routes);
