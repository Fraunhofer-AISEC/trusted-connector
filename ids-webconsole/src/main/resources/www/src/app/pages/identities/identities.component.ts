import {Component} from '@angular/core';

@Component({
  selector: 'identities',
  styles: [],
   template: require('./identities.html') 
})
export class IdentitiesComponent {

  constructor() {
    console.log('Identities created.');
  }
   
}
