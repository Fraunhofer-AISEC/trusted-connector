import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { Identity } from './identity.interface';
import { ESTService } from './est-service';

@Component({
    templateUrl: './identitynewest.component.html'
})
export class NewIdentityESTComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Identity;
    public events: any[] = [];

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title, private readonly estService: ESTService,
                private readonly router: Router) {
        this.titleService.setTitle('New Identity via EST');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            password: '',
            esturl: ''
        });
    }

    public async save(identity: Identity): Promise<boolean> {
       // Call REST to create identity
       this.estService.createIdentity(identity)
            .subscribe(() => undefined);
       return this.router.navigate(['/certificates']);
    }
}
