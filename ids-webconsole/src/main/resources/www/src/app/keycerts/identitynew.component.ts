import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Title } from '@angular/platform-browser';

import { Identity } from './identity.interface';
import { CertificateService } from './keycert.service';

@Component({
    templateUrl: './identitynew.component.html'
})
export class NewIdentityComponent implements OnInit {
    @Output() changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Identity;
    public events: any[] = [];

    constructor(private _fb: FormBuilder, private titleService: Title, private certService: CertificateService, private router: Router) {
        this.titleService.setTitle('New Identity');
    }

    ngOnInit() {
        // the short way to create a FormGroup
        this.myForm = this._fb.group({
            s: ['', <any>Validators.required],
            cn: ['', <any>Validators.required],
            o: '',
            ou: '',
            l: '',
        });
    }

    save(identity: Identity, /*fileInputElement: any,*/ isValid: boolean) {        
         // Call REST to create identity
        this.certService.createIdentity(identity).subscribe();
        this.router.navigate(['/certificates']);
    }
}
