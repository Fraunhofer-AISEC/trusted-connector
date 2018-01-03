import {Component, Input, OnInit} from '@angular/core';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';

import {Result} from '../../result';
import {Route} from '../route';
import {RouteService} from '../route.service';

declare var Viz: any;

@Component({
  selector: 'route-card',
  templateUrl: './route-card.component.html',
  styleUrls: ['./route-card.component.css']
})
export class RouteCardComponent implements OnInit {
  @Input() route: Route;
  vizResult: SafeHtml;
  result: Result;
  statusIcon: string;
  dotPromise: Promise<string>;
  private dotResolver: (dot: string) => void;

  constructor(private sanitizer: DomSanitizer, private routeService: RouteService) {
    this.dotPromise = new Promise ((resolve, reject) => this.dotResolver = resolve);
  }

  get started() {
    return this.route.status == 'Started';
  }

  ngOnInit(): void {
    if(this.route.status == 'Started') {
      this.statusIcon = 'stop';
    } else {
      this.statusIcon = 'play_arrow';
    }
 	  this.dotResolver(this.route.dot);
  }

  onStart(routeId: string): void {
    this.statusIcon = 'play_arrow';
    this.routeService.startRoute(routeId).subscribe(result => {
       this.result = result;
       this.route.status = 'Started';
     });
  }

  onStop(routeId: string): void {
    this.statusIcon = 'stop';
    this.routeService.stopRoute(routeId).subscribe(result => {
       this.result = result;
       this.route.status = 'Stopped';
     });
  }

  onToggle(routeId: string): boolean {
    if (this.statusIcon === 'play_arrow') {
      this.statusIcon = 'stop';
      this.routeService.startRoute(routeId).subscribe(result => {
         this.result = result;
         this.route.status = 'Started';
       });
    } else {
      this.statusIcon = 'play_arrow';
      this.routeService.stopRoute(routeId).subscribe(result => {
         this.result = result;
         this.route.status = 'Stopped';
       });
    }
    return true;
  }
}
