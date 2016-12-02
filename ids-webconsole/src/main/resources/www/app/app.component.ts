import { Component, AfterViewInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

declare var componentHandler: any;

@Component({
  selector: 'iot-connector',
  templateUrl: 'app/app.component.html'
})
export class AppComponent implements AfterViewInit {

  constructor(private titleService: Title) {

  }

  ngAfterViewInit() {
    componentHandler.upgradeAllRegistered();
  }
}
