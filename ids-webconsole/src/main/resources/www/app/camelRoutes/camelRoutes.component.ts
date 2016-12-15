import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { NgModule }      from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { HttpModule }    from '@angular/http';
import { Title } from '@angular/platform-browser'




import { CamelRoutesService } from './camelRoutes.service';
import { Route } from './route';



@Component({
  selector: 'camelRoutes',
  templateUrl: 'app/camelRoutes/camelRoutes.component.html',
   providers: [CamelRoutesService]
})

export class CamelRoutesComponent  implements OnInit{

  title = 'Current Routes';
  routes: Route[];
  selectedRoute: Route;


  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title, private camelRoutesService: CamelRoutesService) {
     this.titleService.setTitle("Camel Routes");
  }

  ngOnInit(): void {
    this.changeTitle.emit('Camel Routes');
    this.getRoutes();
  }

  onSelect(route: Route): void {
      this.selectedRoute = route;
  }

  getRoutes(): void {
    //this.camelRoutesService.getRoutes()//.then(routes => this.routes = routes);

    this.camelRoutesService.getRoutes().subscribe(
                 (routes) => {
                      this.routes = routes;
                      console.log(" routes:  ", this.routes);})
  }

}
