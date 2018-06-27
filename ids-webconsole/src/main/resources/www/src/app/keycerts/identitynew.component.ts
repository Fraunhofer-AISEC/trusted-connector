import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Title } from '@angular/platform-browser';

import { Identity } from './identity.interface';
import { CertificateService } from './keycert.service';

@Component({
    templateUrl: './identitynew.component.html'
})
export class NewIdentityComponent implements OnInit {
    @Output() changeTitle = new EventEmitter();
    myForm: FormGroup;
    data: Identity;
    events: Array<any> = [];

    constructor(private _fb: FormBuilder, private titleService: Title, private certService: CertificateService,
                private router: Router) {
        this.titleService.setTitle('New Identity');
    }

    ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this._fb.group({
            s: ['', Validators.required as any],
            cn: ['', Validators.required as any],
            o: '',
            ou: '',
            l: ''
        });
    }

    save(identity: Identity, /*fileInputElement: any,*/ isValid: boolean): void {
         // Call REST to create identity
        this.certService.createIdentity(identity)
            .subscribe(() => undefined);
        this.router.navigate(['/certificates']);
    }
}
