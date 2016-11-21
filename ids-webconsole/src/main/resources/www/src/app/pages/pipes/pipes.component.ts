import {Component, ViewEncapsulation} from '@angular/core';
import { PipesService } from './pipes.service';

@Component({
  selector: 'pipes',
  encapsulation: ViewEncapsulation.None,
  styles: [],
  template: require('./pipes.html'),
})
export class Pipes {

	source: String;

  constructor(protected service: PipesService) {
    this.service.getRoutesRaw().subscribe(
    	(data) => {
      	this.source = data;
    	},
    	(errData) => {
    		// error
    	},
    	() => {
    		// complete
    	}
    	);
  }

  ngOnInit() {
  }
}
