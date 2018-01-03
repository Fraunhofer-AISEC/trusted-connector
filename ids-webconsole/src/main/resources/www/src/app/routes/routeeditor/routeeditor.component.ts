import { Component, Input, OnInit, ElementRef, HostListener, ViewChild, Renderer2 } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { Route } from '../route';
import { RouteService } from '../route.service';
import { ValidationInfo } from '../validation';

import 'rxjs/add/operator/switchMap';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  private _route: Route = new Route();
  private _validationInfo: ValidationInfo = new ValidationInfo();
  private statusIcon: string;
  private result: string;
  @ViewChild('vizCanvas')
  private vizCanvas: ElementRef;
  private svgElement: HTMLElement;

  public readonly dotPromise: Promise<string>;
  private dotResolver: (dot: string) => void;
  
  constructor(private navRoute: ActivatedRoute, private renderer: Renderer2, private routeService: RouteService) {
    this.dotPromise = new Promise((resolve, reject) => this.dotResolver = resolve);
  }

  get route() {
    return this._route;
  }

  get validationInfo() {
    return this._validationInfo;
  }

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      let id = params.id;

      this.routeService.getRoute(id).subscribe(route => {
        this._route = route;
        console.log("Route editor: Loaded route with id " + this._route.id);

        this.dotResolver(route.dot);

        if(this._route.status == "Started") {
          this.statusIcon = "stop";
        } else {
          this.statusIcon = "play_arrow";
        }
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
