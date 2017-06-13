import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Policy } from './policy';
import { PolicyService } from './policy.service';

@Component({
    templateUrl: './dataflowpoliciesnew.component.html'
})
export class NewDataflowPolicyComponent implements OnInit {
    @Output() changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Policy;
    public submitted: boolean;
    public saved: boolean;
    public events: any[] = [];


    constructor(private _fb: FormBuilder, private titleService: Title, private policyService: PolicyService) {
        this.titleService.setTitle('New Policy');
    }

    ngOnInit() {
        // the short way
        this.myForm = this._fb.group({
            policy_file: ['', [<any>Validators.required, <any>Validators.required]],
        });

        // subscribe to form changes
        this.subcribeToFormChanges();
    }

    ngAfterViewInit() {
    }

    subcribeToFormChanges() {
        const myFormStatusChanges$ = this.myForm.statusChanges;
        const myFormValueChanges$ = this.myForm.valueChanges;

        myFormStatusChanges$.subscribe(x => this.events.push({ event: 'STATUS_CHANGED', object: x }));
        myFormValueChanges$.subscribe(x => {
        this.saved = false;
            this.events.push({ event: 'VALUE_CHANGED', object: x })
        });
    }
}
