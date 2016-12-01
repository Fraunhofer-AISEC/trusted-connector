import { Component, OnInit, ViewContainerRef, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import * as d3 from 'd3';

@Component({
  selector: 'activity',
  template: '<div class="activity-chart" style="min-height: 30px"></div>'
})
export class ActivityComponent implements OnInit {
  element: any;

  constructor(private viewContainerRef:ViewContainerRef) {
     this.element = viewContainerRef.element.nativeElement;
     console.log(this.element);
  }

  ngOnInit(): void {
    console.log(d3);
    d3.select(this.element).select("div").style("background-color", "yellow");
  }
}
