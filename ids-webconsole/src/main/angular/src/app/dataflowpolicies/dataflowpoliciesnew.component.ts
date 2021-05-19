import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Policy } from './policy.interface';
import { PolicyService } from './policy.service';

@Component({
    templateUrl: './dataflowpoliciesnew.component.html'
})
export class NewDataflowPolicyComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Policy;
    public policyFileLabel = 'Select lucon file ...';
    public events: any[] = [];
    public multiple: false;
    public fileUpload: AbstractControl;

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title, private readonly policyService: PolicyService) {
        this.titleService.setTitle('New Policy');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            policyFile: ['', Validators.required]
        });

        this.fileUpload = this.myForm.get('policyFile');
    }

    public save(policy: Policy, fileInputElement: any, _isValid: boolean): void {
        // console.log(policy, fileInputElement, isValid);
        // console.log(fileInputElement.files[0]);

        // Call REST POST to store settings
        this.policyService.install(policy, fileInputElement.files[0])
            .subscribe(() => undefined);
    }

    // Update caption of upload button with file name when a file is selected
    public fileChangeEvent(fileInput: any): void {
        if (fileInput.target.files && fileInput.target.files[0]) {
            this.policyFileLabel = fileInput.target.files[0].name;
        }
    }

}
