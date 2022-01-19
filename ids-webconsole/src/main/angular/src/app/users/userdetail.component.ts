import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { User } from './user.interface';
import { UserService } from './user.service';

@Component({
    templateUrl: './userdetail.component.html'
})
export class DetailUserComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title,
                private readonly userService: UserService,
                private readonly router: Router) {
        this.titleService.setTitle('User details');
    }
    public ngOnInit(): void {
      this.changeTitle.emit('Users');
    }

    // Change Password
    public async save(user: User): Promise<boolean> {
        // set password
        this.userService.createUser(user)  // to replace with setPassword
            .subscribe(() => undefined);
       return this.router.navigate(['/users']);
    }

    // Delete User
    /*
    public async delete(user: User): Promise<boolean> {
        this.userService.deleteUser(user)
            .subscribe(() => undefined);
       return this.router.navigate(['/users']);
    }
    */
}
