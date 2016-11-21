import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule as AngularFormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';

import { Pipes } 		from './pipes.component';
import { PipesService } from './pipes.service';
import { routing }      from './pipes.routing';

@NgModule({
  imports: [
    CommonModule,
    AngularFormsModule,
    NgaModule,
    routing
  ],
  declarations: [
    Pipes
  ],
  providers: [
  	PipesService
  ]
})
export default class PipesModule {
}
