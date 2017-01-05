import { NgModule } from '@angular/core';

// Import Materialize CSS + Directives, like indicated in the readme 
// https://github.com/InfomediaLtd/angular2-materialize
import 'materialize-css';
import 'angular2-materialize';

import { MaterializeDirective } from 'angular2-materialize';

@NgModule({
    declarations: [ MaterializeDirective ],
    exports: [ MaterializeDirective ]
})
export class MaterializeModule { }
