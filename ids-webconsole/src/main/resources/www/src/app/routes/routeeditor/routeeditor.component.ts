import {Component, Input, OnInit} from '@angular/core';
import {FormGroup, FormControl, FormBuilder, Validators} from '@angular/forms';
import {DomSanitizer, SafeHtml, Title} from '@angular/platform-browser';

import {Route} from '../route';
import {RouteService} from '../route.service';
import {ValidationInfo} from '../validation';

import {ActivatedRoute} from '@angular/router';
import 'rxjs/add/operator/switchMap';
import {validateConfig} from '@angular/router/src/config';

declare var Viz: any;

@Component({
  selector: 'routeeditor',
  templateUrl: './routeeditor.component.html',
  styleUrls: ['./routeeditor.component.css']
})
export class RouteeditorComponent implements OnInit {
  public myForm: FormGroup;
  private route: Route = new Route();
  private validationInfo: ValidationInfo = new ValidationInfo();
  private vizResult: SafeHtml;
  private statusIcon: string;
  private result: string;
  private saved: boolean;

  constructor(private titleService: Title, private _fb: FormBuilder, private navRoute: ActivatedRoute, private dom: DomSanitizer, private routeService: RouteService) {
    this.saved = true;
    this.titleService.setTitle('Edit Message Route');
  }

  ngOnInit(): void {
    this.navRoute.params.subscribe(params => {
      let id = params.id;

      this.routeService.getRoute(id).subscribe(route => {
        this.route = route;

        console.log('Route editor: Loaded route with id ' + this.route.id);
        let graph = this.route.dot;

        if (this.route.status === 'Started') {
          this.statusIcon = 'stop';
        } else {
          this.statusIcon = 'play_arrow';
        }

        this.vizResult = this.dom.bypassSecurityTrustHtml(Viz(graph));
      });

      this.routeService.getValidationInfo(id).subscribe(validationInfo => {
        this.validationInfo = validationInfo;
      });
    });

    this.myForm = this._fb.group({
      txtRepresentation: ['', [<any>Validators.required, <any>Validators.minLength(5)]],
    });
  }

  onStart(routeId: string): void {
    this.routeService.startRoute(routeId).subscribe(result => {
      this.result = result;
    });
    this.route.status = 'Started';
    this.statusIcon = 'play_arrow';
  }

  onStop(routeId: string): void {
    this.routeService.stopRoute(routeId).subscribe(result => {
      this.result = result;
    });
    this.route.status = 'Stopped';
    this.statusIcon = 'stop';
  }

  onToggle(routeId: string): void {
    if (this.statusIcon === 'play_arrow') {
      this.statusIcon = 'stop';
      this.routeService.startRoute(routeId).subscribe(result => {
        this.result = result;
      });
      this.route.status = 'Started';

    } else {
      this.statusIcon = 'play_arrow';
      this.routeService.stopRoute(routeId).subscribe(result => {
        this.result = result;
      });

      this.route.status = 'Stopped';
    }
  }

  onRouteDefinitionChanged(newTxtRepresentation: string): void {
    if (!this.route.txtRepresentation || newTxtRepresentation.trim() !== this.route.txtRepresentation.trim()) {
      this.saved = false;
      // Because ace-editor is a nested component, FormBuilder does not hook in properly and we need to save content here.
      this.route.txtRepresentation = newTxtRepresentation;
    } else {
      this.saved = true;
    }
  }

  save(model: any) {
    this.saved = true;

    // Call REST POST to store settings
    let storePromise = this.routeService.save(this.route);
    storePromise.subscribe(
      () => {
        // If saved successfully, user may leave the route (=saved=true)
        this.saved = true;
      },
      error => console.log(error));
  }
}
