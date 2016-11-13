import { Routes, RouterModule }  from '@angular/router';
import { IdentitiesComponent } from './identities.component';

// noinspection TypeScriptValidateTypes
const routes: Routes = [
  {
    path: '',
    component: IdentitiesComponent,
    children: [
      //{ path: 'appcontainers', component: AppContainerTable }
    ]
  }
];

export const routing = RouterModule.forChild(routes);
