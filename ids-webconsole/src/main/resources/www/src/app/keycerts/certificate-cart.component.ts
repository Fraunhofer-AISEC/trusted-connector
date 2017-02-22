import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

declare var Viz: any;

@Component({
  selector: 'certificate-card',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{certificate.file}} -> {{certificate.alias}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        <span>{{certificate.certificate}}</span>
      </div>
      <div class="mdl-card__actions mdl-card--border">
      <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab mdl-button--colored">
        <i class="material-icons">delete</i>
      </button>
      </div>
  `
})
export class CertificateCardComponent implements OnInit {
  @Input() certificate: Certificate;

  constructor(private certificateService: CertificateService) {}

  ngOnInit(): void {
  }
}
