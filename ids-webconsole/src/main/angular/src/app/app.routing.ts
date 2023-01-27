import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from './_guards/auth.guard';
import { AppsComponent } from './apps/apps.component';
import { AppsSearchComponent } from './apps/apps-search.component';
import { ConnectionConfigurationComponent } from './connection-configuration/connection-configuration.component';
import { ConnectionReportComponent } from './connection-report/connection-report.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { DataflowPoliciesComponent } from './dataflowpolicies/dataflowpolicies.component';
import { NewDataflowPolicyComponent } from './dataflowpolicies/dataflowpoliciesnew.component';
import { IdsComponent } from './ids/ids.component';
import { NewIdentityComponent } from './keycerts/identitynew.component';
import { KeycertsComponent } from './keycerts/keycerts.component';
import { HomeLayoutComponent } from './layouts/home-layout/home-layout.component';
import { LoginLayoutComponent } from './layouts/login-layout/login-layout.component';
import { LoginComponent } from './login/login.component';
import { RouteeditorComponent } from './routes/routeeditor/routeeditor.component';
import { UsersComponent } from './users/users.component';
import { NewUserComponent } from './users/usernew.component';
import { DetailUserComponent } from './users/userdetail.component';
import { RoutesComponent } from './routes/routes.component';
import { NewIdentityESTComponent } from './keycerts/identitynewest.component';

const appRoutes: Routes = [
  // Pages using the "home" layout (with sidebar and topnav)
  { path: '', component: HomeLayoutComponent, canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',  component: DashboardComponent,  data: { title: 'Dashboard' }, canActivate: [AuthGuard] },
      { path: 'apps', component: AppsComponent, canActivate: [AuthGuard] },
      { path: 'appsearch', component: AppsSearchComponent, canActivate: [AuthGuard] },
      { path: 'dataflowpolicies', component: DataflowPoliciesComponent, canActivate: [AuthGuard] },
      { path: 'dataflowpolicyynew', component: NewDataflowPolicyComponent, canActivate: [AuthGuard] },
      { path: 'identitynew', component: NewIdentityComponent, canActivate: [AuthGuard] },
      { path: 'connections', component: ConnectionReportComponent, canActivate: [AuthGuard] },
      { path: 'connectionconfiguration', component: ConnectionConfigurationComponent, canActivate: [AuthGuard] },
      { path: 'routes', component: RoutesComponent, canActivate: [AuthGuard] },
      { path: 'routeeditor/:id', component: RouteeditorComponent, canActivate: [AuthGuard] },
      { path: 'routeeditor', component: RouteeditorComponent, canActivate: [AuthGuard] },
      { path: 'ids', component: IdsComponent, canDeactivate: [IdsComponent], canActivate: [AuthGuard] },
      { path: 'users', component: UsersComponent, canActivate: [AuthGuard] },
      { path: 'usernew', component: NewUserComponent, canActivate: [AuthGuard] },
      { path: 'userdetail', component: DetailUserComponent, canActivate: [AuthGuard] },
      { path: 'certificates', component: KeycertsComponent, canActivate: [AuthGuard]  },
      { path: 'identitynewest', component: NewIdentityESTComponent, canActivate: [AuthGuard] }
    ]
  },
  // Pages using the "login" layout (centered full page without sidebar)
  { path: '', component: LoginLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent, data: { title: 'Login' } }
    ]
  }
];

export const routing = RouterModule.forRoot(appRoutes, { useHash: true, relativeLinkResolution: 'legacy' });
