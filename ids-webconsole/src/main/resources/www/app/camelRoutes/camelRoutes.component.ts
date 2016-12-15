import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Route } from './route';
import { Title } from '@angular/platform-browser'

import { CamelRoutesService } from './camelRoutes.service';

@Component({
  selector: 'camelRoutes',
  templateUrl: 'app/camelRoutes/camelRoutes.component.html'
})

export class CamelRoutesComponent  implements OnInit{

  title = 'Current Routes';
  routes: Route[];
  selectedRoute: Route;

  @Output() changeTitle = new EventEmitter();

  constructor(private routeService: CamelRoutesService, private titleService: Title) {
    this.titleService.setTitle("Data Pipes");

    this.routeService.getRoutes().subscribe(routes => {
       this.routes = routes;
     });
  }
  
  ngOnInit(): void {
    this.changeTitle.emit('Camel Routes');
  }

  onSelect(route: Route): void {
      this.selectedRoute = route;
  }
}
