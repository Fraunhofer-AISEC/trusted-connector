import {Component} from '@angular/core';

@Component({
  selector: 'forms',
  styles: [],
  template: `<router-outlet></router-outlet>`
})
export class AppContainer {

  constructor(id: string, image: string, created: string, status: string, ports: string, name: string, size: string, uptime: string) {
    console.log('AppContainer created.');
  }
   
}
