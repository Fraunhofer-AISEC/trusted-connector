import { Component, ElementRef, HostListener, Input, OnInit, Renderer2, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml, Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

import { Result, RouteResult } from '../../result';
import { Route } from '../route';
import { RouteService } from '../route.service';
import { CounterExample, ValidationInfo } from '../validation';

import { ReplaySubject } from 'rxjs/ReplaySubject';
import { validateConfig } from '@angular/router/src/config';
import { Observable } from 'rxjs/Observable';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  myForm: FormGroup;

  private _route?: Route;
  private _textRepresentation?: string;
  private _validationInfo: ValidationInfo = new ValidationInfo();
  private _result: Result = new Result();
  private _saved = true;
  private statusIcon: string;
  @ViewChild('vizCanvas')
  private vizCanvas: ElementRef;
  private svgElement: HTMLElement;
  private _dotSubject: ReplaySubject<string> = new ReplaySubject(1);

  constructor(private titleService: Title, private _fb: FormBuilder, private router: Router,
              private navRoute: ActivatedRoute, private renderer: Renderer2, private routeService: RouteService) {
    this.titleService.setTitle('Edit Message Route');
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

  get textRepresentation(): string {
    return this._textRepresentation;
  }

  set textRepresentation(textRepresentation: string) {
    const trimmedTextRep = textRepresentation.trim();
    this._saved = this._saved && (this._textRepresentation === trimmedTextRep);
    this._textRepresentation = trimmedTextRep;
  }

  get validationInfo(): ValidationInfo {
    return this._validationInfo;
  }

  get result(): Result {
    return this._result;
  }

  get saved(): boolean {
    return this._saved;
  }

  get dotSubject(): ReplaySubject<string> {
    return this._dotSubject;
  }

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      const id = params.id;

      if (!id) {
        return;
      }

      this.routeService.getRoute(id)
        .subscribe(route => {
          this.route = route;
          // console.log('Route editor: Loaded route with id ' + this._route.id);
        });

      this.routeService.getRouteAsString(id)
        .subscribe(routeString => {
          this._textRepresentation = routeString.trim();
        });

      this.routeService.getValidationInfo(id)
        .subscribe(validationInfo => {
          this._validationInfo = validationInfo;
        });
    });

    this.myForm = this._fb.group({
      txtRepresentation: ['', [Validators.required as any, Validators.minLength(5) as any]]
    });
  }

  trackCounterExamples(index: number, item: CounterExample): string {
    return item.explanation + Number(index);
  }

  trackSteps(index: number, item: string): string {
    return item;
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId)
      .subscribe(result => {
        this._result = result;
      });
    this.route.status = 'Started';
    this.statusIcon = 'play_arrow';
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId)
      .subscribe(result => {
        this._result = result;
      });
    this.route.status = 'Stopped';
    this.statusIcon = 'stop';
  }

  onToggle(routeId: string): void {
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

  save(): void {
    this._saved = true;
    const id = this._route.id;

    // Call REST POST/PUT to store route
    if (id) {
      this.routeService.saveRoute(id, this._textRepresentation)
        .subscribe(
          result => {
            // If saved successfully, user may leave the route editor
            this._result = result;
            this._saved = true;
            if (result.successful) {
              // console.log('Route editor: Updated route with id ' + id);
              this.route = result.route;
              this.routeService.getValidationInfo(id)
                .subscribe(validationInfo => {
                  this._validationInfo = validationInfo;
                });
            }
          },
          error => {
            // console.log(error);
          }
        );
    } else {
      this.routeService.addRoute(this._textRepresentation)
        .subscribe(
          result => {
            // If created successfully, redirect user to routes overview
            this._result = result;
            this._saved = true;
            if (result.successful) {
              // console.log('Route editor: Created route(s)');
              this.router.navigate(['routes']);
            }
          },
          error => {
            // console.log(error);
          }
        );
    }
  }
}
