import { Component, EventEmitter, OnInit, Output, ViewContainerRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'keycerts',
    templateUrl: './keycerts.component.html'
})
export class KeycertsComponent implements OnInit {
    title = 'Current Certificates';
    identities: Array<Certificate>;
    certificates: Array<Certificate>;

    @Output() readonly changeTitle = new EventEmitter();

    constructor(private readonly titleService: Title, private readonly certificateService: CertificateService, vcRef: ViewContainerRef) {
        this.titleService.setTitle('Identities');

        this.certificateService.getIdentities()
            .subscribe(identities => {
                this.identities = identities;
            });

        this.certificateService.getCertificates()
            .subscribe(certificates => {
                this.certificates = certificates;
            });
    }

    ngOnInit(): void {
        this.changeTitle.emit('Certificates');
    }

    deleteCert(alias: string): void {
        this.certificateService.deleteCert(alias)
            .subscribe(result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }

    deleteIdentity(alias: string): void {
        this.certificateService.deleteIdentity(alias)
            .subscribe(result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }
    }
