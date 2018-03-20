import { AfterViewInit, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Policy } from './policy.interface';
import { PolicyService } from './policy.service';

declare var componentHandler: any;

@Component({
  templateUrl: './dataflowpolicies.component.html'
})
export class DataflowPoliciesComponent implements OnInit, AfterViewInit {
  @Output() changeTitle = new EventEmitter();
  private _policies?: Array<string>;
  private _isLoaded = false;

  constructor(private titleService: Title, private policyService: PolicyService) {
    this.titleService.setTitle('Data Flow Control');

    this.policyService.getPolicies()
      .subscribe(policies => {
        this._policies = policies;
        this._isLoaded = this._policies && this._policies.length > 0;
      });
  }

  ngOnInit(): void {
    this.changeTitle.emit('Data Flow Control');
  }

  ngAfterViewInit(): void {
    componentHandler.upgradeAllRegistered();
  }

  get policies(): Array<string> {
    return this._policies;
  }

  get isLoaded(): boolean {
    return this._isLoaded;
  }

  trackRules(index: number, item: string): string {
    return item;
  }
}
