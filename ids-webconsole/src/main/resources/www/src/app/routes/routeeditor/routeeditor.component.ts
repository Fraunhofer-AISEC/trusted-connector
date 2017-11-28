import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Route } from '../route';
import { RouteService } from '../route.service';
import { ValidationInfo } from '../validation';

import { ActivatedRoute } from '@angular/router';
import 'rxjs/add/operator/switchMap';
import { validateConfig } from '@angular/router/src/config';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor-widget.html'
})
export class RouteeditorComponent implements OnInit {
  private route: Route = new Route();
  private validationInfo: ValidationInfo = new ValidationInfo();
  private vizResult: SafeHtml;
  private statusIcon: string;
  private result: string;
  
  constructor(private navRoute: ActivatedRoute, private dom: DomSanitizer, private routeService: RouteService) {  }

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      let id = params.id;

      this.routeService.getRoute(id).subscribe(route => {
        this.route = route;

        console.log("Route editor: Loaded route with id " + this.route.id);
        let graph = this.route.dot;
    
        if(this.route.status == "Started") {
          this.statusIcon = "stop";
        } else {
          this.statusIcon = "play_arrow";
        }
    
        this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph));
      });

      this.routeService.getValidationInfo(id).subscribe(validationInfo => {
        this.validationInfo = validationInfo;
      })
    });
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
      this.result = result;
    });
    this.route.status = 'Started';
    this.statusIcon = "play_arrow";
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
       this.result = result;
     });
     this.route.status = 'Stopped';
     this.statusIcon = "stop";
  }

  onToggle(routeId: string): void {
    if(this.statusIcon == "play_arrow") {
      this.statusIcon = "stop";
      this.routeService.startRoute(routeId).subscribe(result => {
        this.result = result;
      });
      this.route.status = 'Started';

    } else {
      this.statusIcon = "play_arrow";
      this.routeService.stopRoute(routeId).subscribe(result => {
        this.result = result;
      });

      this.route.status = 'Stopped';
    }
  }
}
