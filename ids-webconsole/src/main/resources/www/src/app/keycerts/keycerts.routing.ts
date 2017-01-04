import { Routes, RouterModule }  from '@angular/router';
import { KeycertsComponent } from './keycerts.component';

const routes: Routes = [
  {
    path: '',
    component: KeycertsComponent
  }
];

export const routing = RouterModule.forChild(routes);
