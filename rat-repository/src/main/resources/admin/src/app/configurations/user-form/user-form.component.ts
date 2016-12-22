import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

import { User } from '../shared/user';
import { UsersService } from '../shared/users.service';
import { BasicValidators } from '../../shared/basic-validators';

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css']
})
export class UserFormComponent implements OnInit {

  form: FormGroup;
  title: string;
  options = ['BASIC','ADVANCED','ALL'];
  user: User = new User();

  constructor(
    formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private usersService: UsersService
  ) {
    this.form = formBuilder.group({
      name: ['', Validators.compose([Validators.required, Validators.minLength(1), Validators.maxLength(10)])],
      type: ['', Validators.required],
      number: ['', Validators.compose([Validators.required, Validators.minLength(1), Validators.maxLength(2)])],
      value: ['', Validators.compose([Validators.required, Validators.minLength(64), Validators.maxLength(64)])]
    });
  }

  ngOnInit() {
    var id = this.route.params.subscribe(params => {
      var id = params['id'];
      this.title = id ? 'Edit Configuration' : 'New Configuration';
      if (!id) return;
      this.usersService.getUser(id)
        .subscribe(
          user => this.user = user,
          response => {
            if (response.status == 404) {
              this.router.navigate(['NotFound']);
            }
          });
    });
  }

  save() {
    var result, userValue = this.form.value;
    if (userValue.id){
      result = this.usersService.updateUser(userValue);
    } else {
      result = this.usersService.addUser(userValue);
    }
    result.subscribe(data => this.router.navigate(['users']));
  }

  updateConfiguration(event) {
  }
}
