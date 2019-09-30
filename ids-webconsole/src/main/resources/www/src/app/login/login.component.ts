import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { App } from '../apps/app';

import { LoginService } from './login.service';

@Component({
  templateUrl: './login.component.html',
  providers: []
})
export class LoginComponent {
    public form:FormGroup;

    constructor(private fb:FormBuilder,
                 private authService: LoginService,
                 private router: Router) {
        this.form = this.fb.group({
            username: ['',Validators.required],
            password: ['',Validators.required]
        });

  }

  public login(): void {
  console.log('Login clicked');
      const val = this.form.value;

      if (val.username && val.password) {
          this.authService.login(val.username, val.password)
              .subscribe(
                  () => {
                      console.log('User is logged in');
                      this.router.navigateByUrl('/');
                  }
              );
      }
  }
}
