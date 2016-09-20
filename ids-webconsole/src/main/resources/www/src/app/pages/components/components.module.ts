import { NgModule }      from '@angular/core';
import { CommonModule }  from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgaModule } from '../../theme/nga.module';
import { TreeComponent } from 'ng2-tree/index';

import { routing }       from './components.routing';
import { Components } from './components.component';
import { TreeView } from './components/treeView/treeView.component';

// TODO: tree component?
@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgaModule,
    routing
  ],
  declarations: [
    Components,
    TreeView,
    TreeComponent
  ]
})
export default class ComponentsModule {}
