import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { KeycertsComponent } from './keycerts.component';
import { routing } from './keycerts.routing';


@NgModule({
  imports: [
    CommonModule,
    routing
  ],
  declarations: [
    KeycertsComponent,
  ]
})
export default class KeycertsModule {}
