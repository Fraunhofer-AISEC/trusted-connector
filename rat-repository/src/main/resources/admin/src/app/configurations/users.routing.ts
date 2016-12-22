import { Routes, RouterModule } from '@angular/router';

import { UsersComponent } from './users.component';
import {UserFormComponent} from "./user-form/user-form.component";

const usersRoutes: Routes = [
  { path: 'configurations', component: UsersComponent, pathMatch: 'full' },
  { path: 'configurations/new', component: UserFormComponent},
  { path: 'configurations/:id', component: UserFormComponent}
];

export const usersRouting = RouterModule.forChild(usersRoutes);
