import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { Identity } from './identity.interface';
import { CertificateService } from './keycert.service';

@Component({
    templateUrl: './identitynew.component.html'
})
export class NewIdentityComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Identity;
    public events: any[] = [];

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title, private readonly certService: CertificateService,
                private readonly router: Router) {
        this.titleService.setTitle('New Identity');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            s: ['', Validators.required as any],
            cn: ['', Validators.required as any],
            o: '',
            ou: '',
            l: ''
        });
    }

    public async save(identity: Identity): Promise<boolean> {
         // Call REST to create identity
        this.certService.createIdentity(identity)
            .subscribe(() => undefined);

        return this.router.navigate(['/certificates']);
    }
}
