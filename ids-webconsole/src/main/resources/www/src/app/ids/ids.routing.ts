import { Routes, RouterModule }  from '@angular/router';
import { IdsComponent } from './ids.component';

const routes: Routes = [
  {
    path: '',
    component: IdsComponent
  }
];

export const routing = RouterModule.forChild(routes);
