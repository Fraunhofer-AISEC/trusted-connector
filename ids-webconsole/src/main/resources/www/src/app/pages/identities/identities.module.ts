import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';

import { routing }       from './identities.routing';
import { IdentitiesComponent }       from './identities.component';


@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgaModule,
    routing
  ],
  declarations: [
    IdentitiesComponent
  ],
  providers: [
  ]
})
export default class IdentitiesModule {}
