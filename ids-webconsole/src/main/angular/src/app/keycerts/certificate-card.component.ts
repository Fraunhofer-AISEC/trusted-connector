import { Component, Input, OnInit } from '@angular/core';

import { ConfirmService } from '../confirm/confirm.service';

import { Certificate } from './certificate';

declare let componentHandler: any;

@Component({
    selector: 'certificate-card',
    templateUrl: './certificate-card.component.html',
    styleUrls: ['./certificate-card.component.css']
})

export class CertificateCardComponent implements OnInit {
    @Input() public certificates: Certificate[];
    @Input() public trusts: Certificate[];
    @Input() private readonly onDeleteCallback: (alias: string) => void;
    @Input() private readonly onRenewCallback: (alias: string) => void = null;
    public result: string;

    constructor(private readonly confirmService: ConfirmService) {
    }

    public ngOnInit(): void {
        componentHandler.upgradeDom();
    }

    public trackCerts(index: number, item: Certificate): string {
        return item.subjectS + item.subjectCN + item.subjectOU + item.subjectO + item.subjectL + item.subjectC;
    }

    public onRenew(alias: string): void {
        // Sanity check
        if (this.onRenewCallback) {
            this.onRenewCallback(alias);
        }
    }

    public async onDelete(alias: string): Promise<void> {
        return this.confirmService.activate('Are you sure that you want to delete this item?')
            .then(res => {
                if (res) {
                    this.onDeleteCallback(alias);
                }
            });
    }
}
