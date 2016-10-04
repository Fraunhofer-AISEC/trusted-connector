import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule as AngularFormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';

import { Settings } from './settings.component';
import { routing }       from './settings.routing';

import { InlineForm } from './components/inlineForm';

@NgModule({
  imports: [
    CommonModule,
    AngularFormsModule,
    NgaModule,
    routing
  ],
  declarations: [
    Settings,
    InlineForm
  ]
})
export default class SettingsModule {
}
