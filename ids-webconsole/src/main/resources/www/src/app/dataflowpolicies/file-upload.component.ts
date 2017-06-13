import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Http } from '@angular/http';
import { PolicyService } from './policy.service';


@Component({
    selector: 'file-upload',
    template: '<input type="file" [multiple]="multiple" #fileInput>'
})
export class FileUploadComponent {
    @Input() multiple: boolean = false;
    @ViewChild('fileInput') inputEl: ElementRef;

    constructor(private http: Http, private policyService: PolicyService) {}

    upload() {
        console.log("Uploading");
        let inputEl: HTMLInputElement = this.inputEl.nativeElement;
        let fileCount: number = inputEl.files.length;
        let formData = new FormData();
        console.log(fileCount);
        if (fileCount > 0) { // a file was selected
            for (let i = 0; i < fileCount; i++) {
                formData.append('file[]', inputEl.files.item(i));
            }
            console.log("Posting");
            this.policyService.install(formData)
            //this.http
            //    .post('http://your.upload.url', formData)
                // do whatever you do...
                // subscribe to observable to listen for response
        }
    }
}