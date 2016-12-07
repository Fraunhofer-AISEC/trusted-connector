import { Component, Input } from '@angular/core';

import { Route } from './route';

@Component({
  selector: 'route-detail',
  template: `
    <div *ngIf="route">
      
    </div>
  `
})
export class RouteDetailComponent {
  @Input()
  route: Route;
}
