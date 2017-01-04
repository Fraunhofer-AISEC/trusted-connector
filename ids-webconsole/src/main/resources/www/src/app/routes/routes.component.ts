import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { Title } from '@angular/platform-browser'

import { Route } from './route';
import { RouteService } from './route.service';

@Component({
  selector: 'routes',
  templateUrl: './routes.component.html',
})

export class RoutesComponent  implements OnInit{

  title = 'Current Routes';
  routes: Route[];
  selectedRoute: Route;

  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title, private routeService: RouteService) {
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
