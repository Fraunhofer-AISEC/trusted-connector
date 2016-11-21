import { Routes, RouterModule }  from '@angular/router';

import { Pipes } from './pipes.component';

// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: '',
    component: Pipes
  }
];

export const routing = RouterModule.forChild(routes);
