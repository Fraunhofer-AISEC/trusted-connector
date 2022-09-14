import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

//import { User } from './user.interface';
import { UserService } from './user.service';
import { ActivatedRoute } from '@angular/router';

@Component({
    templateUrl: './userdetail.component.html'
})

export class DetailUserComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    userId: string;
    oldPW: string;
    newPW: string;
    rePW: string;
    constructor(private readonly fb: FormBuilder, private readonly titleService: Title,
                private readonly userService: UserService,
                private readonly router: Router,
                private route: ActivatedRoute) {
        this.titleService.setTitle('User settings');
    }
    public ngOnInit(): void {
      this.changeTitle.emit('Users');

      this.myForm = this.fb.group({
        oldpassword: ['', Validators.required as any],
        newpassword: ['', Validators.required as any],
        repeatpassword: ['', Validators.required as any]
      });
      this.userId = this.route.snapshot.queryParamMap.get('user');
    }

    // Change Password
    public async save(): Promise<boolean> {
      this.oldPW = this.myForm.get('oldpassword').value;
      this.newPW = this.myForm.get('newpassword').value;
      this.rePW = this.myForm.get('repeatpassword').value;

      console.log('userpw'+this.oldPW+this.newPW+this.rePW);

      if (this.newPW === this.rePW)
      {
        console.log('changing password');
        this.userService.setPassword(this.userId,this.oldPW,this.newPW);
      }
      else
      {
        console.log('New passwords not equal, password not changed');
      }
      return this.router.navigate(['/users']);
        console.log('userpw'+this.oldPW+this.newPW+this.rePW);
    }
}
