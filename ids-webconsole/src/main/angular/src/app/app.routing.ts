import { mapToCanActivate, RouterModule, Routes } from '@angular/router';

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
import { RenewIdentityESTComponent } from './keycerts/identityrenewest.component';
import { RoutesComponent } from './routes/routes.component';
import { NewIdentityESTComponent } from './keycerts/identitynewest.component';

const guards = mapToCanActivate([AuthGuard]);
const appRoutes: Routes = [
  // Pages using the "home" layout (with sidebar and topnav)
  { path: '', component: HomeLayoutComponent, canActivate: guards,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',  component: DashboardComponent,  data: { title: 'Dashboard' }, canActivate: guards },
      { path: 'apps', component: AppsComponent, canActivate: guards },
      { path: 'appsearch', component: AppsSearchComponent, canActivate: guards },
      { path: 'dataflowpolicies', component: DataflowPoliciesComponent, canActivate: guards },
      { path: 'dataflowpolicyynew', component: NewDataflowPolicyComponent, canActivate: guards },
      { path: 'identitynew', component: NewIdentityComponent, canActivate: guards },
      { path: 'connections', component: ConnectionReportComponent, canActivate: guards },
      { path: 'connectionconfiguration', component: ConnectionConfigurationComponent, canActivate: guards },
      { path: 'routes', component: RoutesComponent, canActivate: guards },
      { path: 'routeeditor/:id', component: RouteeditorComponent, canActivate: guards },
      { path: 'routeeditor', component: RouteeditorComponent, canActivate: guards },
      { path: 'ids', component: IdsComponent, canDeactivate: [IdsComponent], canActivate: guards },
      { path: 'users', component: UsersComponent, canActivate: guards },
      { path: 'usernew', component: NewUserComponent, canActivate: guards },
      { path: 'userdetail', component: DetailUserComponent, canActivate: guards },
      { path: 'certificates', component: KeycertsComponent, canActivate: guards  },
      { path: 'identitynewest', component: NewIdentityESTComponent, canActivate: guards },
      { path: 'identityrenewest/:alias', component: RenewIdentityESTComponent, canActivate: guards }
    ]
  },
  // Pages using the "login" layout (centered full page without sidebar)
  { path: '', component: LoginLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent, data: { title: 'Login' } }
    ]
  }
];

export const routing = RouterModule.forRoot(appRoutes, { useHash: true });
