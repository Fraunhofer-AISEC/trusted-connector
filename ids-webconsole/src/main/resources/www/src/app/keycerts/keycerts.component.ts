import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

@Component({
  selector: 'keycerts',
  templateUrl: 'keycerts.component.html'
})
export class KeycertsComponent implements OnInit{

  title = 'Current Certificates';
  certificates: Certificate[];
  trusts: Certificate[];

  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title, private certificateService: CertificateService) {
    this.titleService.setTitle('Certificates');

    this.certificateService.getCertificates().subscribe(certificates => {
       this.certificates = certificates;
       this.trusts = certificates;	//TODO Replace by trust store certs.
     });
  }

  ngOnInit(): void {
    this.changeTitle.emit('Certificates');
  }

}
