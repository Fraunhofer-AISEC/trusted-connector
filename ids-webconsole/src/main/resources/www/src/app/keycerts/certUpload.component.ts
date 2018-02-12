import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Http, Headers } from '@angular/http';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'file-uploading',
    templateUrl: './certUpload.component.html'
})
export class CertUploadComponent {
    @Input() multiple = false;
    @ViewChild('fileIn') inputEl: ElementRef;

    constructor(private http: Http, private certificateService: CertificateService) {}

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
