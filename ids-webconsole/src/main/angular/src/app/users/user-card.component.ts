import { Component, Input, OnInit } from '@angular/core';

import { ConfirmService } from '../confirm/confirm.service';

import { User } from './user';

declare let componentHandler: any;

@Component({
  selector: 'user-card',
  templateUrl: './user-card.component.html',
  styleUrls: ['./user-card.component.css']
})

export class UserCardComponent implements OnInit {
  @Input() public users: User[];
  @Input() private readonly onDeleteCallback: (username: string) => void;
  public result: string;

  constructor(private readonly confirmService: ConfirmService) { }

  public ngOnInit(): void {
    componentHandler.upgradeDom();
  }

  public trackUsers(index: number, item: User): string {
    return item.username;
  }

  public async onDelete(username: string): Promise<void> {
    return this.confirmService.activate('Are you sure that you want to delete this user?')
      .then(res => {
        if (res) {
          this.onDeleteCallback(username);
        }
      });
  }
}
