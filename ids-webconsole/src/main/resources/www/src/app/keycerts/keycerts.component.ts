import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

@Component({
  selector: 'keycerts',
  templateUrl: './keycerts.component.html'
})
export class KeycertsComponent implements OnInit{
  title = 'Current Certificates';
  identities: Certificate[];
  certificates: Certificate[];

  @Output() changeTitle = new EventEmitter();

  constructor(private titleService: Title, private certificateService: CertificateService) {
    this.titleService.setTitle('Certificates');

    this.certificateService.getIdentities().subscribe(identities => {
       this.identities = identities;
     });

    this.certificateService.getCertificates().subscribe(certificates => {
       this.certificates = certificates;
     });
  }

  ngOnInit(): void {
    this.changeTitle.emit('Certificates');
    this.file = null;
  }

  file: File;
  onChange(event: EventTarget) {
        let eventObj: MSInputMethodContext = <MSInputMethodContext> event;
        let target: HTMLInputElement = <HTMLInputElement> eventObj.target;
        let files: FileList = target.files;
        if (this.file === null) {
        this.file = files[0];
        console.log(this.file);
      }

    }

    onUpload() {
      if (this.file !== null) {
        this.certificateService.uploadCert(this.file);
        this.file = null;
          console.log("file is uploaded");
      }
    }

}
