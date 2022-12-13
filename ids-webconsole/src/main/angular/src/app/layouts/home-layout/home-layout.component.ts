import { Component, OnInit, AfterViewInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { LoginService } from '../../login/login.service';

declare let componentHandler: any;

@Component({
  selector: 'app-home-layout',
  templateUrl: './home-layout.component.html',
  styles: [],
  providers: [
      Title,
      LoginService
  ]
})
export class HomeLayoutComponent implements AfterViewInit, OnInit  {
  public isLoggedIn:   boolean;

  constructor(public titleService: Title, public loginService: LoginService,  private router: Router) {}

  public ngAfterViewInit(): void {
    componentHandler.upgradeAllRegistered();
  }

    public ngOnInit(): void {
      this.isLoggedIn = sessionStorage.getItem('currentUser') != null;
    }

  get title(): string {
    return this.titleService.getTitle();
  }

  public logout(): void {
    this.loginService.logout();
    this.router.navigateByUrl('/login');
  }
}
