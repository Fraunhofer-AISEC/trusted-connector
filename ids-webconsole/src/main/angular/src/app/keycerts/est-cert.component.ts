import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { Identity } from './identity.interface';
import { ESTService } from './est-service';

@Component({
    templateUrl: './est-cert.component.html'
})
export class ESTCertComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public myForm: FormGroup;
    public data: Identity;
    public events: any[] = [];

    constructor(private readonly fb: FormBuilder, private readonly titleService: Title, private readonly estService: ESTService,
                private readonly router: Router) {
        this.titleService.setTitle('Set EST CA cert');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            ESTUrl: ['', Validators.required as any],
            certificateHash: '',
            certificate: ''
        });
    }

    public async requestEstCaCert(url: string, hash: string): Promise<void> {
         await this.estService.requestEstCaCert(url, hash).subscribe(e => {this.myForm.patchValue({
                                                                                          certificate: e
                                                                                          });
                                                                              });

        }

    public async onGetCertBtnClick(): Promise<void> {
         this.requestEstCaCert(this.myForm.get('ESTUrl')?.value,this.myForm.get('certificateHash')?.value);

    }

    public saveEstCert(): void {
         this.estService.uploadCert(this.myForm.get('certificate')?.value);
          this.router.navigate(['/certificates'])
              .then(() => {
              window.location.reload();
          });
}

}
