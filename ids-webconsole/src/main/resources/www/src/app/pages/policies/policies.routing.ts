import { Routes, RouterModule }  from '@angular/router';

import { Policies } from './policies.component';

// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: '',
    component: Policies
  }
];

export const routing = RouterModule.forChild(routes);
