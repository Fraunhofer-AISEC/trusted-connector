import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Policy } from './policy.interface';
import { PolicyService } from './policy.service';
import { AbstractControl } from '@angular/forms/src/model';

@Component({
    templateUrl: './dataflowpoliciesnew.component.html'
})
export class NewDataflowPolicyComponent implements OnInit {
    @Output() changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Policy;
    public policyFileLabel = 'Select lucon file ...';
    public events: any[] = [];
    public multiple: false;
    public fileUpload: AbstractControl;

    constructor(private _fb: FormBuilder, private titleService: Title, private policyService: PolicyService) {
        this.titleService.setTitle('New Policy');
    }

    ngOnInit() {
        // the short way to create a FormGroup
        this.myForm = this._fb.group({
            policy_file: ['', <any>Validators.required],
        });

        this.fileUpload = this.myForm.get('policy_file');
    }

    save(policy: Policy, fileInputElement: any, isValid: boolean) {
        console.log(policy, fileInputElement, isValid);
        console.log(fileInputElement.files[0]);

         // Call REST POST to store settings
        return this.policyService.install(policy, fileInputElement.files[0]).subscribe();
    }

    // Update caption of upload button with file name when a file is selected
    fileChangeEvent(fileInput: any) {
        if (fileInput.target.files && fileInput.target.files[0]) {
            this.policyFileLabel = fileInput.target.files[0].name;
        }
    }

}
