import { Component, ElementRef, Input, ViewChild } from '@angular/core';

import { CertificateService } from './keycert.service';

@Component({
    selector: 'file-uploading',
    templateUrl: './cert-upload.component.html'
})
export class CertUploadComponent {
    @Input() public multiple = false;
    @ViewChild('fileIn') public inputEl: ElementRef;

    constructor(private readonly certificateService: CertificateService) {}

    public upload(): void {
        const inputEl: HTMLInputElement = this.inputEl.nativeElement;
        const fileCount: number = inputEl.files.length;

        if (fileCount > 0) { // a file was selected

            for (let i = 0; i < fileCount; i++) {
                this.certificateService.uploadCert(inputEl.files.item(i));
            }

            location.reload();
        }
    }
}
