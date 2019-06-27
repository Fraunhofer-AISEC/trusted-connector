import { Component, Input, OnInit } from '@angular/core';

import { ConfirmService } from '../confirm/confirm.service';

import { Certificate } from './certificate';

declare var componentHandler: any;

@Component({
  selector: 'certificate-card',
  templateUrl: './certificate-card.component.html',
  styleUrls: ['./certificate-card.component.css']
})

export class CertificateCardComponent implements OnInit {
  @Input() certificates: Array<Certificate>;
  @Input() trusts: Array<Certificate>;
  result: string;
  @Input() private readonly onDeleteCallback: Function;

  constructor(private readonly confirmService: ConfirmService) { }

  ngOnInit(): void {
    componentHandler.upgradeDom();
  }

  trackCerts(index: number, item: Certificate): string {
    return item.subjectCN + item.subjectOU + item.subjectO + item.subjectL;
  }

  async onDelete(alias: string): Promise<void> {
    return this.confirmService.activate('Are you sure that you want to delete this item?')
      .then(res => {
        if (res) {
          this.onDeleteCallback(alias);
        }
      });
  }
}
