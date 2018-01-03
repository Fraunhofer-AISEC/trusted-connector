import { Component, AfterViewInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

declare var componentHandler: any;

@Component({
  selector: 'iot-connector',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements AfterViewInit {

  constructor(public titleService: Title) {}

  ngAfterViewInit() {
    componentHandler.upgradeAllRegistered();
  }
}
