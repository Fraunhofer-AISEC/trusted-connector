import { Component, OnInit, AfterViewInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { LoginService } from '../../login/login.service';

@Component({
  selector: 'app-login-layout',
  templateUrl: 'login-layout.component.html',
  styles: [],
  providers: [
      Title,
      LoginService
  ]
})
export class LoginLayoutComponent implements OnInit  {
  public isLoggedIn:   boolean;

  constructor(public titleService: Title, public loginService: LoginService) {}

    public ngOnInit(): void {
      this.isLoggedIn = localStorage.getItem('currentUser') != null;
    }

  get title(): string {
    return this.titleService.getTitle();
  }
}
