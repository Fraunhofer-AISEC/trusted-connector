import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Route } from './route';
import { Title } from '@angular/platform-browser'


@Component({
  selector: 'camelRoutes',
  templateUrl: 'app/camelRoutes/camelRoutes.component.html'
})

export class CamelRoutesComponent  implements OnInit{

  title = 'Current Routes';
  //routes = ;
  selectedRoute: Route;

  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title) {
     this.titleService.setTitle("Camel Routes");
  }

  ngOnInit(): void {
    this.changeTitle.emit('Camel Routes');
  }

  onSelect(route: Route): void {
      this.selectedRoute = route;
  }
}
