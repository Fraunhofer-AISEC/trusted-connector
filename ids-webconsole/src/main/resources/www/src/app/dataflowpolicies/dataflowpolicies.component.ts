import {Component, OnInit, AfterViewInit, EventEmitter, Output} from '@angular/core';
import {Title} from '@angular/platform-browser';

import {Policy} from './policy.interface';
import {PolicyService} from './policy.service';

declare var componentHandler: any;

@Component({
  templateUrl: './dataflowpolicies.component.html'
})
export class DataflowPoliciesComponent implements OnInit, AfterViewInit {
  @Output() changeTitle = new EventEmitter();
  policies: string[];
  isLoaded: boolean;

  constructor(private titleService: Title, private policyService: PolicyService) {
    this.titleService.setTitle('Data Flow Control');

    this.policyService.getPolicies().subscribe(policies => {
      this.policies = policies;
      this.isLoaded = this.policies != null && this.policies.length > 0;
    });

  }

  ngOnInit(): void {
    this.changeTitle.emit('Data Flow Control');
  }

  ngAfterViewInit(): void {
    componentHandler.upgradeAllRegistered();
  }
}
