import { AfterViewInit, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { PolicyService } from './policy.service';

declare let componentHandler: any;

@Component({
  templateUrl: './dataflowpolicies.component.html'
})
export class DataflowPoliciesComponent implements AfterViewInit {
  private _policies?: string[];
  private _isLoaded = false;

  constructor(private readonly titleService: Title, private readonly policyService: PolicyService) {
    this.titleService.setTitle('Data Flow Control');

    this.policyService.getPolicies()
      .subscribe(policies => {
        this._policies = policies;
        this._isLoaded = this._policies && this._policies.length > 0;
      });
  }

  public ngAfterViewInit(): void {
    componentHandler.upgradeAllRegistered();
  }

  get policies(): string[] {
    return this._policies;
  }

  get isLoaded(): boolean {
    return this._isLoaded;
  }

  public trackRules(index: number, item: string): string {
    return item;
  }
}
