import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup ,Validators} from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { User } from './user.interface';
import { UserService } from './user.service';

@Component({
    templateUrl: './usernew.component.html'
})
export class NewUserComponent implements OnInit {
    public userForm: UntypedFormGroup;
    public data: User;

    constructor(private readonly fb: UntypedFormBuilder, private readonly titleService: Title,
                private readonly userService: UserService,
                private readonly router: Router) {
        this.titleService.setTitle('New User');
    }
    public ngOnInit(): void {
      this.userForm = this.fb.group({
          username: ['', Validators.required as any],
          password: ['', Validators.required as any]
      });
    }

    public async save(user: User): Promise<void> {
        await this.userService.createUser(user);
        await this.router.navigate(['/users']);
    }



}
