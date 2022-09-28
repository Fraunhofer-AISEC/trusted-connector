import { AfterViewInit, Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';

import { App } from './app';
import { AppService } from './app.service';

declare let componentHandler: any;

@Component({
  templateUrl: './apps-search.component.html',
  providers: []
})
export class AppsSearchComponent implements OnInit, AfterViewInit {
    public myForm: UntypedFormGroup;
    public submitted: boolean;
    public saved: boolean;
    public searchResults: App[] = [];

    constructor(private readonly fb: UntypedFormBuilder, private readonly appService: AppService) {
        this.saved = true;
        this.submitted = false;
    }

    public ngOnInit(): void {
        // the short way
        this.myForm = this.fb.group({
            apps_search: ['', [Validators.required as any, Validators.minLength(3) as any]]
        });
    }

    public ngAfterViewInit(): void {
        componentHandler.upgradeAllRegistered();
    }

    public save(model: any, _isValid: boolean): void {
      this.submitted = true;
      this.appService
        .searchApps(model.apps_search)
        .subscribe(res => { this.searchResults = res; });
    }

    public trackApps(index: number, item: App): string {
      return item.id;
    }
}
