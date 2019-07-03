import { AfterViewInit, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

declare var componentHandler: any;

@Component({
  selector: 'iot-connector',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements AfterViewInit {

  constructor(public titleService: Title) {}

  public ngAfterViewInit(): void {
    componentHandler.upgradeAllRegistered();
  }

  get title(): string {
    return this.titleService.getTitle();
  }
}
