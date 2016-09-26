import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';

import { AppContainer } from './appcontainers.component';
import { AppContainerTable } from './components/appcontainerTable/appcontainerTable.component';
import { routing }       from './appcontainers.routing';
import { Ng2SmartTableModule } from 'ng2-smart-table';
import { AppContainerService } from './components/appcontainerTable/appcontainerTable.service';


@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgaModule,
    Ng2SmartTableModule,
    routing
  ],
  declarations: [
    AppContainer,
    AppContainerTable
  ],
  providers: [
    AppContainerService
  ]
})
export default class AppContainerModule {}
