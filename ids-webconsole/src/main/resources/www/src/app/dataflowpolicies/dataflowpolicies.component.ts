import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Policy } from './policy';
import { PolicyService } from './policy.service';

@Component({
  templateUrl: './dataflowpolicies.component.html'
})
export class DataflowPoliciesComponent implements OnInit {
  @Output() changeTitle = new EventEmitter();
  policies: Policy[];
    
  constructor(private titleService: Title, private policyService: PolicyService) {
     this.titleService.setTitle('Data Usage Control');
     
      this.policyService.getPolicies().subscribe(policies => {
       this.policies = policies;
     });
  }

  ngOnInit(): void {
    this.changeTitle.emit('Data Usage Control');
  }
}
