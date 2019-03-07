import { AfterViewInit, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { App } from './app';
import { AppService } from './app.service';

declare var componentHandler: any;

@Component({
  templateUrl: './apps.component.html',
  providers: []
})
export class AppsComponent implements AfterViewInit {
  apps: Array<App>;

  constructor(private readonly appService: AppService, private readonly titleService: Title) {
    this.titleService.setTitle('Apps');

    this.appService.getApps()
      .subscribe(apps => {
        this.apps = apps;
      });
  }

  ngAfterViewInit(): void {
        componentHandler.upgradeAllRegistered();
   }

  trackApps(index: number, item: App): string {
    return item.id;
  }
}

@Component({
  templateUrl: './apps-search.component.html',
  providers: []
})
export class AppsSearchComponent implements OnInit, AfterViewInit {
    myForm: FormGroup;
    submitted: boolean;
    saved: boolean;
    searchResults: Array<App> = [];

    constructor(private readonly _fb: FormBuilder, private readonly _appService: AppService) {
        this.saved = true;
        this.submitted = false;
    }

    ngOnInit(): void {
        // the short way
        this.myForm = this._fb.group({
            apps_search: ['', [Validators.required as any, Validators.minLength(3) as any]]
        });
    }

    ngAfterViewInit(): void {
        componentHandler.upgradeAllRegistered();
    }

    save(model: any, isValid: boolean): void {
      this.submitted = true;
      this._appService
        .searchApps(model.apps_search)
        .subscribe(res => { this.searchResults = res; });
    }

    trackApps(index: number, item: App): string {
      return item.id;
    }
}
