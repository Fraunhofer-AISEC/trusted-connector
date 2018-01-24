import {Component, Input, OnInit, ElementRef, HostListener, ViewChild, Renderer2} from '@angular/core';
import {DomSanitizer, SafeHtml, Title} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {FormGroup, FormControl, FormBuilder, Validators} from '@angular/forms';

import {Result} from '../../result';
import {Route} from '../route';
import {RouteService} from '../route.service';
import {ValidationInfo} from '../validation';

import 'rxjs/add/operator/switchMap';
import {validateConfig} from '@angular/router/src/config';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  private _route: Route = new Route();
  private _textRepresentation: string = null;
  private _validationInfo: ValidationInfo = new ValidationInfo();
  public myForm: FormGroup;
  private _result: Result = new Result();
  private _saved = true;
  private statusIcon: string;
  @ViewChild('vizCanvas')
  private vizCanvas: ElementRef;
  private svgElement: HTMLElement;

  public readonly dotPromise: Promise<string>;
  private dotResolver: (dot: string) => void;

  constructor(private titleService: Title, private _fb: FormBuilder, private router: Router,
    private navRoute: ActivatedRoute, private renderer: Renderer2, private routeService: RouteService) {
    this.titleService.setTitle('Edit Message Route');
    this.dotPromise = new Promise((resolve, reject) => this.dotResolver = resolve);
  }

  get route() {
    return this._route;
  }

  get textRepresentation() {
    return this._textRepresentation;
  }

  set textRepresentation(textRepresentation: string) {
    let trimmedTextRep = textRepresentation.trim();
    this._saved = this._saved && (this._textRepresentation === trimmedTextRep);
    if (!this._saved) {
      console.log('Need to save text: ' + trimmedTextRep);
    }
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

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      let id = params.id;

      if (!id) {
        return;
      }

      this.routeService.getRoute(id).subscribe(route => {
        this._route = route;
        console.log('Route editor: Loaded route with id ' + this._route.id);

        this.dotResolver(route.dot);

        if (this._route.status === 'Started') {
          this.statusIcon = 'stop';
        } else {
          this.statusIcon = 'play_arrow';
        }
      });

      this.routeService.getRouteAsString(id).subscribe(routeString => {
        this._textRepresentation = routeString.trim();
      })

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

    // Call REST POST to store settings
    let storePromise = this.routeService.save(this._textRepresentation);
    storePromise.subscribe(
      (result) => {
        // If saved successfully, user may leave the route (=saved=true)
        this._result = result;
        this._saved = true;
        if (result.successful) {
          this.router.navigate(['routes']);
        }
      },
      error => {
       console.log(error);
      });
  }
}
