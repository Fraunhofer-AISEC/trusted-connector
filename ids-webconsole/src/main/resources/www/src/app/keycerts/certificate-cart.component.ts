import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

import {PrettifyPipe} from '../prettify-json.pipe';

declare var Viz: any;

@Component({
  selector: 'certificate-card',
  template: `
      <div class="mdl-card__title mdl-card--expand">
        <h2 class="mdl-card__title-text">{{certificate.file}} -> {{certificate.alias}}</h2>
      </div>
      <div class="mdl-card__supporting-text">
        <pre innerHTML = "{{ certificate.certificate | prettify }}"></pre>
      </div>
      <div class="mdl-card__actions mdl-card--border">
      <button class="mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab mdl-button--colored" (click)="onDelete(certificate.alias, certificate.file)">
        <i class="material-icons">delete</i>
      </button>
      </div>
  `
})
export class CertificateCardComponent implements OnInit {
  @Input() certificate: Certificate;
  result: string;

  constructor(private certificateService: CertificateService) {}

  ngOnInit(): void {
  }

  onDelete(alias: string, file: string): void {
    this.certificateService.deleteEntry(alias, file).subscribe(result => {
       this.result = result;
       console.log("result:" + this.result + "==");

       if(result.toString() == 'true') {
          location.reload();
        } else {
           console.log("okkkkk: " + result);
        }
     });


  }
}
