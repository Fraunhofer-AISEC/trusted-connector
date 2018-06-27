import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Policy } from './policy.interface';
import { PolicyService } from './policy.service';
import { AbstractControl } from '@angular/forms/src/model';

@Component({
    templateUrl: './dataflowpoliciesnew.component.html'
})
export class NewDataflowPolicyComponent implements OnInit {
    @Output() changeTitle = new EventEmitter();
    myForm: FormGroup;
    data: Policy;
    policyFileLabel = 'Select lucon file ...';
    events: Array<any> = [];
    multiple: false;
    fileUpload: AbstractControl;

    constructor(private _fb: FormBuilder, private titleService: Title, private policyService: PolicyService) {
        this.titleService.setTitle('New Policy');
    }

    ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this._fb.group({
            policy_file: ['', Validators.required as any]
        });

        this.fileUpload = this.myForm.get('policy_file');
    }

    save(policy: Policy, fileInputElement: any, isValid: boolean): void {
        // console.log(policy, fileInputElement, isValid);
        // console.log(fileInputElement.files[0]);

        // Call REST POST to store settings
        this.policyService.install(policy, fileInputElement.files[0])
            .subscribe(() => undefined);
    }

    // Update caption of upload button with file name when a file is selected
    fileChangeEvent(fileInput: any): void {
        if (fileInput.target.files && fileInput.target.files[0]) {
            this.policyFileLabel = fileInput.target.files[0].name;
        }
    }

}
