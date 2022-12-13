import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { LoginService } from './login/login.service';

declare let componentHandler: any;

@Component({
  selector: 'iot-connector',
  template: '<router-outlet></router-outlet>',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements AfterViewInit, OnInit  {
  public isLoggedIn:   boolean;

  constructor(public titleService: Title, public loginService: LoginService) {}

  public ngAfterViewInit(): void {
    componentHandler.upgradeAllRegistered();
  }

    public ngOnInit(): void {
      this.isLoggedIn = sessionStorage.getItem('currentUser') != null;
    }

  get title(): string {
    return this.titleService.getTitle();
  }

}
