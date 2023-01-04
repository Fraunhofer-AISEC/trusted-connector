import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

import { LoginService } from './login.service';

@Component({
    templateUrl: './login.component.html',
    providers: []
})
export class LoginComponent {
    public form: UntypedFormGroup;
    public errorText: string = undefined;

    constructor(private fb: UntypedFormBuilder,
        private authService: LoginService,
        private router: Router,
        private route: ActivatedRoute
    ) {
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
                    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
                    return this.router.navigateByUrl(returnUrl);
                }, () => {
                    this.errorText = 'Login rejected, wrong username or password?';
                });
        } else {
            // Use empty string to indicate empty username/password error
            this.errorText = '';
        }
    }
}
