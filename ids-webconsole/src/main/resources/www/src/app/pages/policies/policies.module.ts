import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule as AngularFormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';

import { Policies } 		from './policies.component';
import { PoliciesService } from './policies.service';
import { routing }      from './policies.routing';

@NgModule({
  imports: [
    CommonModule,
    AngularFormsModule,
    NgaModule,
    routing
  ],
  declarations: [
    Policies
  ],
  providers: [
  	PoliciesService
  ]
})
export default class PoliciesModule {
}
