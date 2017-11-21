import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from '../route';
import { RouteService } from '../route.service';

import { ActivatedRoute } from '@angular/router';
import 'rxjs/add/operator/switchMap';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor-widget.html'
})
export class RouteeditorComponent implements OnInit {
  private camelRoute: Route = new Route();
  vizResult: SafeHtml;
  statusIcon: string;
  result: string;
  private id: string = null;  // Camel Route Id
  private camelRoutes: Route[] = null;
  
  constructor(private navRoute: ActivatedRoute, private dom: DomSanitizer, private routeService: RouteService) {  }

  ngOnInit(): void {
    // Load route parameter. This is done by an observable because router may not recreate this component
    this.navRoute.params.subscribe(params => {
      this.id = params["id"];
      this.updateRoute();
    });
    this.routeService.getRoutes().subscribe(camelRoutes => {
      this.camelRoutes = camelRoutes;
      this.updateRoute();
    });
  }
   
  updateRoute() {
    // missing data
    if (this.id == null || this.camelRoutes == null) {
      return;
    }

    this.camelRoutes.forEach(r => {
      if (r.id == this.id) {
        this.camelRoute = r;
      }
    });

    console.log("Route editor: Loaded route with id " + this.camelRoute.id);
    let graph = this.camelRoute.dot;

    if(this.camelRoute.status == "Started") {
      this.statusIcon = "stop";
    } else {
      this.statusIcon = "play_arrow";
    }

    this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph));
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
      this.result = result;
    });
    this.camelRoute.status = 'Started';
    this.statusIcon = "play_arrow";
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.camelRoute.status = 'Stopped';
     this.statusIcon = "stop";
  }

  onToggle(routeId: string): void {
    if(this.statusIcon == "play_arrow") {
      this.statusIcon = "stop";
      this.routeService.startRoute(routeId).subscribe(result => {
        this.result = result;
      });
      this.camelRoute.status = 'Started';

    } else {
      this.statusIcon = "play_arrow";
      this.routeService.stopRoute(routeId).subscribe(result => {
        this.result = result;
      });

      this.camelRoute.status = 'Stopped';
    }
  }
}
