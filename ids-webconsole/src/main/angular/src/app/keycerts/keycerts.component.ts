import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'keycerts',
    templateUrl: './keycerts.component.html'
})
export class KeycertsComponent {
    public title = 'Current Certificates';
    public identities: Certificate[];
    public certificates: Certificate[];

    constructor(private readonly titleService: Title, private readonly certificateService: CertificateService) {
        this.titleService.setTitle('Certificates');

        this.certificateService.getIdentities()
            .subscribe(identities => {
                this.identities = identities;
            });

        this.certificateService.getCertificates()
            .subscribe(certificates => {
                this.certificates = certificates;
            });
    }

    public deleteCert = (alias: string) => {
        this.certificateService.deleteCert(alias)
            .subscribe(result => {
                if (result === alias) {
                    location.reload();
                }
            });
    };

    public deleteIdentity = (alias: string) => {
        this.certificateService.deleteIdentity(alias)
            .subscribe(result => {
                if (result === alias) {
                    location.reload();
                }
            });
    };
}
