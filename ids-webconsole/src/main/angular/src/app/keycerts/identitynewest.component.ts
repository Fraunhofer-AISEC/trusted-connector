import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { Identity } from './identity.interface';

import {v4 as uuidv4} from 'uuid';
import { ESTService } from './est-service';

@Component({
    templateUrl: './identitynewest.component.html'
})
export class NewIdentityESTComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Identity;
    public events: any[] = [];
    private readonly estService: ESTService;

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title, /*private readonly estService: ESTService,*/
                private readonly router: Router) {
        this.titleService.setTitle('New Identity via EST');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            s: ['', Validators.required as any],
            cn: [uuidv4(), Validators.required as any],
            o: '',
            ou: '',
            l: '',
            username: '',
            password: '',
            esturl: ''
        });
    }

    public async save(identity: Identity): Promise<boolean> {
       // Call REST to create identity
       this.estService.createIdentity(identity,
       this.myForm.get('username')?.value,
       this.myForm.get('password')?.value,
       this.myForm.get('esturl')?.value)
            .subscribe(() => undefined);

       return this.router.navigate(['/certificates']);
    }
}
