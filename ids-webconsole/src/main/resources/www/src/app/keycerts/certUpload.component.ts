import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Http, Headers } from '@angular/http';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'file-uploading',
    template: '<input accept=".crt,.der,.cer, .jks, .cert" type="file" [multiple]="multiple" #fileIn>'
})
export class CertUploadComponent {
    @Input() multiple: boolean = false;
    @ViewChild('fileIn') inputEl: ElementRef;

    constructor(private http: Http,private certificateService: CertificateService) {}

    upload(keystoreDestination: string) {
        let inputEl: HTMLInputElement = this.inputEl.nativeElement;
        let fileCount: number = inputEl.files.length;
        let formData = new FormData();
        let headers = new Headers();
        headers.append('Content-Type', 'multipart/form-data');
        
        if (fileCount > 0) { // a file was selected
             
            for (let i = 0; i < fileCount; i++) {
                this.certificateService.uploadCert(inputEl.files.item(i), keystoreDestination);
            }
           
            location.reload();
        }
    }
}