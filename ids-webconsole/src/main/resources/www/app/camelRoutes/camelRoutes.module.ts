import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule }   from '@angular/forms';

import { CamelRoutesComponent } from './camelRoutes.component';
import { RouteDetailComponent } from './route-detail.component';
import { routing } from './camelRoutes.routing';


@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    routing
  ],
  declarations: [
    CamelRoutesComponent,
    RouteDetailComponent
  ]
})
export default class CamelRouteModule {}
