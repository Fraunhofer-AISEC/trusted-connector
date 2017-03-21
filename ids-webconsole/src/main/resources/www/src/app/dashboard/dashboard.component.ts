import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { RouteService } from '../routes/route.service';
import { Title } from '@angular/platform-browser';

@Component({
  templateUrl: './dashboard.component.html',
  providers: []
})
export class DashboardComponent implements OnInit {
  @Output() changeTitle = new EventEmitter();
  private camelComponents: any;

  constructor(private titleService: Title, private routeService: RouteService) {
     this.titleService.setTitle('Dashboard');
  }

  ngOnInit(): void {
    this.changeTitle.emit('Dashboard');
    this.routeService.listComponents().subscribe(result => {this.camelComponents = result});
    console.log(this.camelComponents);
  }
}
