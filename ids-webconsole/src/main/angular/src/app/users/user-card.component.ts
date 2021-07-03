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
  @Input() public trusts: User[];
  @Input() private readonly onDeleteCallback: (alias: string) => void;
  public result: string;

  constructor(private readonly confirmService: ConfirmService) { }

  public ngOnInit(): void {
    componentHandler.upgradeDom();
  }

  public trackCerts(index: number, item: User): string {
    return item.username;
  }

  public async onDelete(alias: string): Promise<void> {
    return this.confirmService.activate('Are you sure that you want to delete this user?')
      .then(res => {
        if (res) {
          this.onDeleteCallback(alias);
        }
      });
  }
}
