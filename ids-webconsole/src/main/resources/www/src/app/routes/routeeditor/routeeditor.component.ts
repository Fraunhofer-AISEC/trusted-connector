import { Component, Input, OnInit, ElementRef, HostListener, ViewChild, Renderer2 } from '@angular/core';
import { DomSanitizer, SafeHtml, Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

import { Result, RouteResult } from '../../result';
import { Route } from '../route';
import { RouteService } from '../route.service';
import { ValidationInfo } from '../validation';

import 'rxjs/add/operator/switchMap';
import { ReplaySubject } from 'rxjs';
import { validateConfig } from '@angular/router/src/config';
import { Observable } from 'rxjs/Observable';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  public myForm: FormGroup;

  private _route: Route = new Route();
  private _textRepresentation: string = null;
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

  get route() {
    return this._route;
  }

  set route(route: Route) {
    this._route = route;

    // Update viz graph
    this._dotSubject.next(route.dot);

    // Update icon
    if (this._route.status === 'Started') {
      this.statusIcon = 'stop';
    } else {
      this.statusIcon = 'play_arrow';
    }
  }

  get textRepresentation() {
    return this._textRepresentation;
  }

  set textRepresentation(textRepresentation: string) {
    let trimmedTextRep = textRepresentation.trim();
    this._saved = this._saved && (this._textRepresentation === trimmedTextRep);
    this._textRepresentation = trimmedTextRep;
  }

  get validationInfo() {
    return this._validationInfo;
  }

  get result() {
    return this._result;
  }

  get saved() {
    return this._saved;
  }

  get dotSubject() {
    return this._dotSubject;
  }

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      let id = params.id;

      if (!id) {
        return;
      }

      this.routeService.getRoute(id).subscribe(route => {
        this.route = route;
        console.log('Route editor: Loaded route with id ' + this._route.id);
      });

      this.routeService.getRouteAsString(id).subscribe(routeString => {
        this._textRepresentation = routeString.trim();
      });

      this.routeService.getValidationInfo(id).subscribe(validationInfo => {
        this._validationInfo = validationInfo;
      });
    });

    this.myForm = this._fb.group({
      txtRepresentation: ['', [<any>Validators.required, <any>Validators.minLength(5)]],
    });
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
      this._result = result;
    });
    this.route.status = 'Started';
    this.statusIcon = 'play_arrow';
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
      this._result = result;
    });
    this.route.status = 'Stopped';
    this.statusIcon = 'stop';
  }

  onToggle(routeId: string): void {
    if (this.statusIcon === 'play_arrow') {
      this.statusIcon = 'stop';
      this.routeService.startRoute(routeId).subscribe(result => {
        this._result = result;
      });
      this.route.status = 'Started';

    } else {
      this.statusIcon = 'play_arrow';
      this.routeService.stopRoute(routeId).subscribe(result => {
        this._result = result;
      });

      this.route.status = 'Stopped';
    }
  }

  save(model: any) {
    this._saved = true;
    let id = this._route.id;

    // Call REST POST/PUT to store route
    if (id) {
      this.routeService.saveRoute(id, this._textRepresentation).subscribe(
        result => {
          // If saved successfully, user may leave the route editor
          this._result = result;
          this._saved = true;
          if (result.successful) {
            console.log('Route editor: Updated route with id ' + id);
            this.route = result.route;
            this.routeService.getValidationInfo(id).subscribe(validationInfo => {
              this._validationInfo = validationInfo;
            });
          }
        },
        error => {
          console.log(error);
        }
      );
    } else {
      this.routeService.addRoute(this._textRepresentation).subscribe(
        result => {
          // If created successfully, redirect user to routes overview
          this._result = result;
          this._saved = true;
          if (result.successful) {
            console.log('Route editor: Created route(s)');
            this.router.navigate(['routes']);
          }
        },
        error => {
          console.log(error);
        }
      );
    }
  }
}
