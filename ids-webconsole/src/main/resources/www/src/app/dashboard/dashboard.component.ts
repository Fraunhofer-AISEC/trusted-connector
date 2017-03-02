import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

@Component({
  templateUrl: './dashboard.component.html',
  providers: []
})
export class DashboardComponent implements OnInit {
  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title) {
     this.titleService.setTitle('Dashboard');
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
  }
}
