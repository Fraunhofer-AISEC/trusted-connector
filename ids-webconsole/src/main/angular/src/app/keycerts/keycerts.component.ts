import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Certificate } from './certificate';
import { CertificateService } from './keycert.service';

@Component({
    selector: 'keycerts',
    templateUrl: './keycerts.component.html'
})
export class KeycertsComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public title = 'Current Certificates';
    public identities: Certificate[];
    public certificates: Certificate[];

    constructor(private readonly titleService: Title, private readonly certificateService: CertificateService) {
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
            .subscribe(_result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }

    public deleteIdentity(alias: string): void {
        this.certificateService.deleteIdentity(alias)
            .subscribe(_result => {
                //             this.result = result;
                //             if(result.toString() === "true") {
                //                location.reload();
                //              }
            });
    }
    }
