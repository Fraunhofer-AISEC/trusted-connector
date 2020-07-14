import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { of, timer } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';

import { SettingsService } from './settings.service';
import { TermsOfService } from './terms-of-service.interface';

@Component({
    selector: 'my-app',
    templateUrl: './ids.component.html',
    styleUrls: ['./ids.component.css'],
    providers: [SettingsService]
})
export class IdsComponent implements OnInit {
    public settingsForm?: FormGroup;
    public saved = true;
    public tosWebconsole?: TermsOfService;

    constructor(private readonly settingsService: SettingsService, private readonly formBuilder: FormBuilder) { }

    public canDeactivate(target: IdsComponent): boolean {
        return target.saved;
    }

    public ngOnInit(): void {
        // Pull settings from server
        this.settingsService.getSettings()
            .subscribe(response => {
                // Initialize form
                this.settingsForm = this.formBuilder.group({
                    appstoreUrl: response.appstoreUrl,
                    brokerUrl: response.brokerUrl,
                    ttpHost: response.ttpHost,
                    ttpPort: response.ttpPort,
                    acmeServerWebcon: [response.acmeServerWebcon, [], [control =>
                        timer(500)
                            .pipe(
                                switchMap(() => {
                                    const value: string = control.value;

                                    return value === '' ? of(undefined) : this.settingsService.getToS(value);
                                }),
                                map(tos => {
                                    this.tosWebconsole = tos;

                                    return tos && tos.error ? { asyncError: tos.error } : undefined;
                                }),
                                take(1)
                            )
                    ]],
                    acmeDnsWebcon: response.acmeDnsWebcon,
                    acmePortWebcon: response.acmePortWebcon,
                    tosAcceptWebcon: response.tosAcceptWebcon,
                    dapsUrl: response.dapsUrl,
                    keystoreName: response.keystoreName,
                    keystorePassword: response.keystorePassword,
                    keystoreAliasName: response.keystoreAliasName,
                    truststoreName: response.truststoreName

                });
                this.subscribeToFormChanges();
            });
    }

    public subscribeToFormChanges(): void {
        this.settingsForm.valueChanges.subscribe(_ => {
            this.saved = false;
        });
    }

    public save(): void {
        if (this.settingsForm.valid) {
            this.settingsService.store(this.settingsForm.value)
                .subscribe(() => this.saved = true);
        }
    }
}
