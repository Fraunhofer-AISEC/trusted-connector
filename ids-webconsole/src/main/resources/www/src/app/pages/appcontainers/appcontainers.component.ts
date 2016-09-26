import {Component} from '@angular/core';

@Component({
  selector: 'forms',
  styles: [],
  template: `<router-outlet></router-outlet>`
})
export class AppContainer {

  constructor(id: string, name: string, image: string, size: number) {
    console.log('AppContainer created.');
  }
   
}
