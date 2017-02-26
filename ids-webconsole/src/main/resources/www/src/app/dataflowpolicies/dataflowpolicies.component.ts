import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

@Component({
  templateUrl: 'dataflowpolicies.component.html',
  providers: []
})
export class DataflowpoliciesComponent implements OnInit {
  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title) {
     this.titleService.setTitle('Data Usage Control');
  }

  ngOnInit(): void {
    this.changeTitle.emit('Data Usage Control');
  }
}
