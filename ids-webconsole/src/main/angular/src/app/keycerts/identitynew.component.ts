import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import {v4 as uuidv4} from 'uuid';

import { Identity } from './identity.interface';
import { ESTService } from './est-service';

@Component({
    templateUrl: './identitynew.component.html'
})
export class NewIdentityComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Identity;
    public events: any[] = [];

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title, private readonly estService: ESTService,
                private readonly router: Router) {
        this.titleService.setTitle('New Identity');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            s: [uuidv4(), Validators.required as any],
            cn: ['', Validators.required as any],
            o: '',
            ou: '',
            l: '',
            username: '',
            password: ''
        });
        console.log('here');
        console.log(uuidv4());
    }

    public async save(identity: Identity): Promise<boolean> {
         // Call REST to create identity
        this.estService.createIdentity(identity,this.myForm.get('username')?.value,this.myForm.get('password')?.value)
            .subscribe(() => undefined);

        return this.router.navigate(['/certificates']);
    }
}
