import { Component, Input, OnInit, ElementRef } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ReplaySubject } from 'rxjs/ReplaySubject';

import { Route } from '../route';
import { RouteService } from '../route.service';
import { ValidationInfo } from '../validation';

import { ActivatedRoute } from '@angular/router';
import 'rxjs/add/operator/switchMap';
import { validateConfig } from '@angular/router/src/config';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  private _route: Route = new Route();
  private _validationInfo: ValidationInfo = new ValidationInfo();
  private _vizResult: SafeHtml;
  private statusIcon: string;
  private result: string;
  public routeSubject: ReplaySubject<Route> = new ReplaySubject(1);
  
  constructor(private navRoute: ActivatedRoute, private dom: DomSanitizer, private routeService: RouteService) { }

  get route() {
    return this._route;
  }

  get validationInfo() {
    return this._validationInfo;
  }

  get vizResult() {
    return this._vizResult;
  }

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      let id = params.id;

      this.routeService.getRoute(id).subscribe(route => {
        this._route = route;
        console.log("Send route...");
        this.routeSubject.next(route);
        console.log("Route editor: Loaded route with id " + this._route.id);

        if(this._route.status == "Started") {
          this.statusIcon = "stop";
        } else {
          this.statusIcon = "play_arrow";
        }

        this._vizResult = this.dom.bypassSecurityTrustHtml(Viz(this._route.dot));
      });

      this.routeService.getValidationInfo(id).subscribe(validationInfo => {
        this._validationInfo = validationInfo;
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
