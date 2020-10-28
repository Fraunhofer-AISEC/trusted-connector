import { Component, EventEmitter, OnInit, Output, ViewContainerRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'keycerts',
    templateUrl: './keycerts.component.html'
})
export class KeycertsComponent implements OnInit {
    public title = 'Current Certificates';
    public identities: Certificate[];
    public certificates: Certificate[];

    @Output() public readonly changeTitle = new EventEmitter();

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

    public ngOnInit(): void {
        this.changeTitle.emit('Certificates');
    }

    public deleteCert(alias: string): void {
        this.certificateService.deleteCert(alias)
            .subscribe(result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }

    public deleteIdentity(alias: string): void {
        this.certificateService.deleteIdentity(alias)
            .subscribe(result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }
    }
