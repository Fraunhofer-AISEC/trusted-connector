import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ESTService } from './est-service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    templateUrl: './identityrenewest.component.html'
})
export class RenewIdentityESTComponent {
    estUrl = 'https://daps-dev.aisec.fraunhofer.de';
    rootCertHash = '7d3f260abb4b0bfa339c159398c0ab480a251faa385639218198adcad9a3c17d';

    constructor(private readonly titleService: Title,
                private readonly estService: ESTService,
                private readonly router: Router,
                private readonly route: ActivatedRoute) {
        this.titleService.setTitle('Renew Identity via the EST');
    }

    onSubmit() {
        this.estService.renewIdentity({
            estUrl: this.estUrl,
            rootCertHash: this.rootCertHash,
            alias: this.route.snapshot.paramMap.get('alias')
        }).subscribe(() => this.router.navigate([ '/certificates' ]));
    }
}
