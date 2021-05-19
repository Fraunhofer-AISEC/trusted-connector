import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';

import { LoginService } from './login.service';

@Component({
    templateUrl: './login.component.html',
    providers: []
})
export class LoginComponent {
    public form: FormGroup;
    public errorText: string = undefined;

    constructor(private fb: FormBuilder,
        private authService: LoginService,
        private router: Router) {
        this.form = this.fb.group({
            username: [''],
            password: ['']
        });

    }

    public clearError(): void {
        this.errorText = undefined;
    }

    public login(): void {
        const val = this.form.value;

        if (val.username && val.password) {
            this.authService.login(val.username, val.password)
                .subscribe(() => {
                    // console.log('User is logged in');
                    this.router.navigateByUrl('/');
                }, () => {
                    this.errorText = 'Login rejected, wrong username or password?';
                });
        } else {
            // Use empty string to indicate empty username/password error
            this.errorText = '';
        }
    }
}
