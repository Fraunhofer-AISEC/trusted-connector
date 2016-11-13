import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';

import { AppContainer } from './appcontainers.component';
import { AppContainerTable } from './components/appcontainerTable/appcontainerTable.component';
import { AppContainerMasonry } from './components/appcontainerMasonry/appcontainerMasonry.component';
import { routing }       from './appcontainers.routing';
import { MasonryModule } from 'angular2-masonry';
import { Ng2SmartTableModule } from 'ng2-smart-table';
//import { AppContainerService } from './components/appcontainerTable/appcontainerTable.service';
import { AppContainerMasonryService } from './components/appcontainerMasonry/appcontainerMasonry.service';


@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgaModule,
    Ng2SmartTableModule,
    routing,
    MasonryModule
  ],
  declarations: [
    AppContainer,
    AppContainerTable,
    AppContainerMasonry
  ],
  providers: [
    AppContainerMasonryService
  ]
})
export default class AppContainerModule {}
