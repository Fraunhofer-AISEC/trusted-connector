import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

import { PrettifyPipe } from '../prettify-json.pipe';

import { ConfirmService } from '../confirm/confirm.service';

declare var componentHandler: any;

@Component({
  selector: 'certificate-card',
  templateUrl: './certificate-card.component.html',
  styleUrls: ['./certificate-card.component.css']
})

export class CertificateCardComponent implements OnInit {
  @Input() certificates: Certificate[];
  @Input() trusts: Certificate[];
  result: string;

  @Input() private onDeleteCallback: Function;

  constructor(private certificateService: CertificateService, private confirmService: ConfirmService) { }

  ngOnInit(): void {
    componentHandler.upgradeDom();
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
