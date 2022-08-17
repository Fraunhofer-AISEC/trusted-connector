import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { ReplaySubject } from 'rxjs';

import { Result } from '../../result';
import { Route } from '../route';
import { RouteService } from '../route.service';
import { CounterExample } from '../counter-example';
import { ValidationInfo } from '../validation-info';

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  private _route?: Route;
  private _newRoute = false;
  private _validationInfo: ValidationInfo = new ValidationInfo();
  private _result: Result = new Result();
  private statusIcon: string;
  private readonly _dotSubject: ReplaySubject<string> = new ReplaySubject(1);

  constructor(private readonly titleService: Title, private readonly navRoute: ActivatedRoute,
              private readonly routeService: RouteService) {
    this.titleService.setTitle('Edit Message Route');
  }

  get routeUpMinutes(): string {
    return (this.route.uptime / 1000 / 60).toFixed();
  }

  get route(): Route {
    return this._route;
  }

  set route(route: Route) {
    this._route = route;

    // Update viz graph
    this._dotSubject.next(route.dot);

    // Update icon
    this.statusIcon = (this._route.status === 'Started') ? 'stop' : 'play_arrow';
  }

  get newRoute(): boolean {
    return this._newRoute;
  }

  get validationInfo(): ValidationInfo {
    return this._validationInfo;
  }

  get result(): Result {
    return this._result;
  }

  get dotSubject(): ReplaySubject<string> {
    return this._dotSubject;
  }

  public ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      const id = params.id;

      if (!id) {
        this._newRoute = true;

        return;
      }

      this.routeService.getRoute(id)
        .subscribe(route => {
          this.route = route;
          // console.log('Route editor: Loaded route with id ' + this._route.id);
        });

      this.routeService.getValidationInfo(id)
        .subscribe(validationInfo => {
          this._validationInfo = validationInfo;
        });
    });
  }

  public trackCounterExamples(index: number, item: CounterExample): string {
    return `${item.explanation}${Number(index)}`;
  }

  public trackSteps(index: number, item: string): string {
    return item;
  }

  public onStart(routeId: string): void {
    this.routeService.startRoute(routeId)
      .subscribe(result => {
        this._result = result;
      });
    this.route.status = 'Started';
    this.statusIcon = 'play_arrow';
  }

  public onStop(routeId: string): void {
    this.routeService.stopRoute(routeId)
      .subscribe(result => {
        this._result = result;
      });
    this.route.status = 'Stopped';
    this.statusIcon = 'stop';
  }

  public onToggle(routeId: string): void {
    if (this.statusIcon === 'play_arrow') {
      this.statusIcon = 'stop';
      this.routeService.startRoute(routeId)
        .subscribe(result => {
          this._result = result;
        });
      this.route.status = 'Started';

    } else {
      this.statusIcon = 'play_arrow';
      this.routeService.stopRoute(routeId)
        .subscribe(result => {
          this._result = result;
        });

      this.route.status = 'Stopped';
    }
  }
}
