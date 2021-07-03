import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
//import {of, timer } from 'rxjs'; //
//import { map, switchMap, take } from 'rxjs/operators';//

import { User } from './user';
import { UserService } from './user.service';
import { Title } from '@angular/platform-browser';
@Component({
    selector: 'users',
    templateUrl: './users.component.html',
    styleUrls: ['./users.component.css'],
    providers: [UserService]
})
export class UsersComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public settingsForm?: FormGroup;
    public saved = true;
    public users: User[];

    constructor(private readonly titleService: Title, private readonly userService: UserService,
                private readonly formBuilder: FormBuilder) {
      this.titleService.setTitle('Users');
      this.userService.getUsers()
                  .subscribe(users => {
                      this.users = users;
                  });
    }


    public canDeactivate(target: UsersComponent): boolean {
        return target.saved;
    }

    public deleteUser(alias: string): void {
        this.userService.deleteUser(alias)
            .subscribe(_result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }

    public ngOnInit(): void {
        this.changeTitle.emit('Users');
    }
}
