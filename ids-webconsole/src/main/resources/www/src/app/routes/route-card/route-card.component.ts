import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Result } from '../../result';
import { Route } from '../route';
import { RouteService } from '../route.service';
import { ReplaySubject } from 'rxjs';

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
  dotSubject: ReplaySubject<string> = new ReplaySubject(1);

  constructor(private sanitizer: DomSanitizer, private routeService: RouteService) {}

  get started(): boolean {
    return this.route.status === 'Started';
  }

  ngOnInit(): void {
    this.statusIcon = this.route.status === 'Started' ? 'stop' : 'play_arrow';
    this.dotSubject.next(this.route.dot);
  }

  onStart(routeId: string): void {
    this.statusIcon = 'play_arrow';
    this.routeService.startRoute(routeId)
      .subscribe(result => {
        this.result = result;
        this.route.status = 'Started';
      });
  }

  onStop(routeId: string): void {
    this.statusIcon = 'stop';
    this.routeService.stopRoute(routeId)
      .subscribe(result => {
        this.result = result;
        this.route.status = 'Stopped';
      });
  }

  onToggle(routeId: string): void {
    if (this.statusIcon === 'play_arrow') {
      this.statusIcon = 'stop';
      this.routeService.startRoute(routeId)
        .subscribe(result => {
          this.result = result;
          this.route.status = 'Started';
        });
    } else {
      this.statusIcon = 'play_arrow';
      this.routeService.stopRoute(routeId)
        .subscribe(result => {
          this.result = result;
          this.route.status = 'Stopped';
        });
    }
  }
}
