import { Component, Input, OnInit } from '@angular/core';

import { Certificate } from './certificate';
import { ConfirmService } from '../confirm/confirm.service';

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
  @Input() private onDeleteCallback: Function;

  constructor(private confirmService: ConfirmService) { }

  ngOnInit(): void {
    componentHandler.upgradeDom();
  }

  trackCerts(index: number, item: Certificate): string {
    return item.subjectCN + item.subjectOU + item.subjectO + item.subjectL;
  }

  onDelete(alias: string): void {
    this.confirmService.activate('Are you sure that you want to delete this item?')
      .then(res => {
        if (res) {
          this.onDeleteCallback(alias);
        }
      });
  }
}
