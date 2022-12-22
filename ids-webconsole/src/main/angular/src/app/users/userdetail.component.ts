import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { UserService } from './user.service';
import { ActivatedRoute } from '@angular/router';

@Component({
    templateUrl: './userdetail.component.html'
})
export class DetailUserComponent implements OnInit {
    public myForm: FormGroup;
    userId: string;
    oldPW: string;
    newPW: string;
    rePW: string;

    constructor(private readonly fb: FormBuilder,
                private readonly titleService: Title,
                private readonly userService: UserService,
                private readonly router: Router,
                private route: ActivatedRoute) {
        this.titleService.setTitle('User settings');
    }

    public ngOnInit(): void {
        this.myForm = this.fb.group({
            oldpassword: ['', Validators.required],
            newpassword: ['', Validators.required],
            repeatpassword: ['', Validators.required]
        });
        this.userId = this.route.snapshot.queryParamMap.get('user');
    }

    // Change Password
    public async save(): Promise<boolean> {
        this.oldPW = this.myForm.get('oldpassword').value;
        this.newPW = this.myForm.get('newpassword').value;
        this.rePW = this.myForm.get('repeatpassword').value;

        if (this.newPW === this.rePW) {
            console.log('changing password');
            await this.userService.setPassword(this.userId, this.oldPW, this.newPW);
        } else {
            console.log('New passwords not equal, password not changed');
        }
        return this.router.navigate(['/users']);
    }
}
