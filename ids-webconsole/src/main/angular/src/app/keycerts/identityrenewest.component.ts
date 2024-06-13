import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ESTService } from './est-service';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
    templateUrl: './identityrenewest.component.html'
})
export class RenewIdentityESTComponent {
    estUrl = 'https://daps-dev.aisec.fraunhofer.de';
    rootCertHash = '7d3f260abb4b0bfa339c159398c0ab480a251faa385639218198adcad9a3c17d';
    error = null;

    constructor(private readonly titleService: Title,
                private readonly estService: ESTService,
                private readonly router: Router,
                private readonly route: ActivatedRoute) {
        this.titleService.setTitle('Renew Identity via the EST');
    }

    handleError(err: HttpErrorResponse) {
        if (err.status === 0) {
            this.error = 'Network Error';
        } else {
            const errObj = JSON.parse(err.error);
            if (errObj.message) {
                this.error = errObj.message;
            } else {
                // Errors have no message if it is disabled by the spring application
                this.error = `Error response from connector: ${err.status}: ${errObj.error}`;
            }
        }
    }

    onSubmit() {
        this.estService.renewIdentity({
            estUrl: this.estUrl,
            rootCertHash: this.rootCertHash,
            alias: this.route.snapshot.paramMap.get('alias')
        }).subscribe(
            () => this.router.navigate([ '/certificates' ]),
            err => this.handleError(err)
        );
    }
}
