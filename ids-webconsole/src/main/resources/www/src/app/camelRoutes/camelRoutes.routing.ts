import { Routes, RouterModule }  from '@angular/router';
import { CamelRoutesComponent } from './camelRoutes.component';

const routes: Routes = [
  {
    path: '',
    component: CamelRoutesComponent
  }
];

export const routing = RouterModule.forChild(routes);
