import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Http, Headers } from '@angular/http';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'file-uploading',
    template: '<label class="mdl-button mdl-js-button mdl-button--icon mdl-color--white mdl-color-text--black"><input accept=".crt,.der,.cer, .jks, .cert" type="file" [multiple]="multiple" #fileIn style="display:none">            <i class="material-icons">add</i></label>'
})
export class CertUploadComponent {
    @Input() multiple: boolean = false;
    @ViewChild('fileIn') inputEl: ElementRef;

    constructor(private http: Http,private certificateService: CertificateService) {}

    upload() {
        let inputEl: HTMLInputElement = this.inputEl.nativeElement;
        let fileCount: number = inputEl.files.length;
        let formData = new FormData();
        let headers = new Headers();
        headers.append('Content-Type', 'multipart/form-data');
        
        if (fileCount > 0) { // a file was selected
             
            for (let i = 0; i < fileCount; i++) {
                this.certificateService.uploadCert(inputEl.files.item(i));
            }
           
            location.reload();
        }
    }
}