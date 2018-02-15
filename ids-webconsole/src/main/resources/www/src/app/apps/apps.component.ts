import { Component } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { App, DockerHubApp } from './app';
import { AppService } from './app.service';
import { AppCardComponent } from './app-card.component';

@Component({
  templateUrl: './apps.component.html',
  providers: []
})
export class AppsComponent {
  apps: Array<App>;

  constructor(private appService: AppService, private titleService: Title) {
    this.titleService.setTitle('Apps');

    this.appService.getApps()
      .subscribe(apps => {
        this.apps = apps;
      });
  }

  trackApps(index: number, item: App): string {
    return item.id;
  }
}

@Component({
  templateUrl: './apps-search.component.html',
  providers: []
})
export class AppsSearchComponent {
    myForm: FormGroup;
    submitted: boolean;
    saved: boolean;
    searchResults: Array<DockerHubApp> = [];

    constructor(private _fb: FormBuilder, private _appService: AppService) {
        this.saved = true;
        this.submitted = false;
    }

    ngOnInit(): void {
        // the short way
        this.myForm = this._fb.group({
            apps_search: ['', [Validators.required as any, Validators.minLength(3) as any]],
        });
    }

    save(model: any, isValid: boolean): void {
        this.submitted = true;
        console.log(model, isValid);

		this._appService.searchApps(model.apps_search).subscribe(
		(res) => {
			this.searchResults = res;
		});
    }

  trackApps(index: number, item: App): string {
    return item.id;
  }    
}
