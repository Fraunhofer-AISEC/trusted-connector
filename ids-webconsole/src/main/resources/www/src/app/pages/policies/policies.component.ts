import {Component, ViewEncapsulation} from '@angular/core';
import { PoliciesService } from './policies.service';

@Component({
  selector: 'policies',
  encapsulation: ViewEncapsulation.None,
  styles: [],
  template: require('./policies.html'),
})
export class Policies {

	source: String;

  constructor(protected service: PoliciesService) {
    this.service.getPolicies().subscribe(
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
