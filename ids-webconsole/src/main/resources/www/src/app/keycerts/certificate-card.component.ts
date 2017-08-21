import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

import {PrettifyPipe} from '../prettify-json.pipe';

import {ConfirmService} from "../confirm/confirm.service";

declare var componentHandler:any;

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
		    <span class="mdl-list__item-secondary-content" style="text-align:right">
		      <a class="mdl-list__item-secondary-action mdl-color-text--grey-600" (click)="onDelete(certificate.alias)"><i class="material-icons">delete</i></a>
		    </span>
	    </li>
    </ul>
  `
})

export class CertificateCardComponent implements OnInit {
  @Input() certificates: Certificate[];
  @Input() ondelete: Function;
  @Input() trusts: Certificate[];
  result: string;

  constructor(private certificateService: CertificateService, private confirmService:ConfirmService) {}

  ngOnInit(): void {
    componentHandler.upgradeDom();
  }

  onDelete(alias: string): void {
    this.confirmService.activate("Are you sure that you want to delete this item?")
        .then(res => {
          if (res == true) {
              this.ondelete(alias);
            //this.certificateService.deleteEntry(alias, file).subscribe(result => {
            // this.result = result;
            // if(result.toString() === "true") {
            //    location.reload();
            //  }
           //});
    }});
  }
}
