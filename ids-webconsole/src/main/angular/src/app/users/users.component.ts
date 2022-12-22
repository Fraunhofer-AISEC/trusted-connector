import { AfterViewInit, Component } from '@angular/core';

import { UserService } from './user.service';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

declare let componentHandler: any;

@Component({
    templateUrl: './users.component.html'
})
export class UsersComponent implements AfterViewInit {
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

    get users(): string[] {
      return this._users;
    }

    public async onDeleteBtnClick(userId: string) {
        await this.userService.deleteUser(userId);
        window.location.reload();
    }

    public async onSettingsBtnClick(userId: string): Promise<boolean> {
        return this.router.navigate(['/userdetail'], { queryParams: { user: userId } });
    }

    get isLoaded(): boolean {
      return this._isLoaded;
    }

    public trackUsers(index: number, item: string): string {
      return item;
    }


}
