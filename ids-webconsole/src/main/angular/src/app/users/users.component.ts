import { AfterViewInit, Component } from '@angular/core';

import { UserService } from './user.service';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

declare let componentHandler: any;

@Component({
    templateUrl: './users.component.html'
})
export class UsersComponent implements AfterViewInit {
    public saved = true;
    private _users?: string[];
    private _isLoaded = false;

    constructor(private readonly titleService: Title, private readonly userService: UserService, private readonly router: Router) {
      this.titleService.setTitle('Users');

      this.userService.getUsers()
           .subscribe(users => {
             this._users = users;
             this._isLoaded = this._users && this._users.length > 0;
           });
    }

    public ngAfterViewInit(): void {
      componentHandler.upgradeAllRegistered();
    }

    public canDeactivate(target: UsersComponent): boolean {
        return target.saved;
    }

    get users(): string[] {
      return this._users;
    }

    public onDeleteBtnClick(userId: string): void {
          this.userService.deleteUser(userId).subscribe(() => undefined);
          //window.location.reload();
           this.router.navigate(['/users'])
                 .then(() => {
                     window.location.reload();
                   });
    }

    public onSettingsBtnClick(userId: string): void {
          //window.location.reload();
          console.log('settingsclick');
           this.router.navigate(['/userdetail'],{ queryParams: {user: userId}});
                 //.then(() => {
                     //window.location.reload();
                   //});
    }

    public deleteUser(username: string): void {
        this.userService.deleteUser(username);
            //.subscribe(_result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            // });
    }

    get isLoaded(): boolean {
      return this._isLoaded;
    }

    public trackUsers(index: number, item: string): string {
      return item;
    }


}
