import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

declare var Viz: any;

@Component({
  selector: 'certificate-card',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{certificate.title}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        {{certificate.description}}
      </div>`
})
export class CertificateCardComponent implements OnInit {
  @Input() certificate: Certificate;

  constructor(private certificateService: CertificateService) {}

  ngOnInit(): void {
  }
}
