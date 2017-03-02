import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

import {PrettifyPipe} from '../prettify-json.pipe';

@Component({
  selector: 'certificate-card',
  template: `
    <ul class="mdl-list">
	    <li class="mdl-list__item mdl-list__item--three-line" *ngFor="let certificate of certificates">
		    <span class="mdl-list__item-primary-content">
		      <i class="material-icons mdl-list__item-avatar">person</i>
		      <span>{{certificate.subjectCN}}</span>
		      <span class="mdl-list__item-text-body">
		        {{certificate.subjectOU}} {{certificate.subjectO}} {{certificate.subjectL}}
		      </span>
		    </span>
		    <span class="mdl-list__item-secondary-content">
		      <a class="mdl-list__item-secondary-action mdl-color-text--grey-600" href="#"><i class="material-icons">open_in_browser</i></a>
			</span>
		    <span class="mdl-list__item-secondary-content">
		      <a class="mdl-list__item-secondary-action mdl-color-text--grey-600" href="#"><i class="material-icons">delete</i></a>
		    </span>
	    </li>
    </ul>
  `
})
export class CertificateCardComponent implements OnInit {
  @Input() certificates: Certificate[];
  @Input() trusts: Certificate[];
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
           console.log("ok: " + result);
        }
     });


  }
}
