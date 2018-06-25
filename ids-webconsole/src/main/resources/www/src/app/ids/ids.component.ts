import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { SettingsService } from './settings.service';
import { TermsOfService } from './terms-of-service.interface';
import { map, switchMap, take } from 'rxjs/operators';
import { timer } from 'rxjs';

@Component({
    selector: 'my-app',
    templateUrl: './ids.component.html',
    styleUrls: ['./ids.component.css'],
    providers: [SettingsService]
})
export class IdsComponent implements OnInit {
    settingsForm?: FormGroup;
    saved = true;
    tosWebconsole?: TermsOfService;

    constructor(private settingsService: SettingsService, private formBuilder: FormBuilder) { }

    canDeactivate(target: IdsComponent): boolean {
        return target.saved;
    }

    ngOnInit(): void {
        // Pull settings from server
        this.settingsService.getSettings()
            .subscribe(response => {
                // Initialize form
                this.settingsForm = this.formBuilder.group({
                    brokerUrl: response.brokerUrl,
                    ttpHost: response.ttpHost,
                    ttpPort: response.ttpPort,
                    acmeServerWebcon: [response.acmeServerWebcon, [], [control =>
                        timer(300)
                            .pipe(
                                switchMap(_ => this.settingsService.getToS(control.value)),
                                map(tos => {
                                    this.tosWebconsole = tos;

                                    return tos.error ? { asyncError: tos.error } : undefined;
                                }),
                                take(1)
                            )]],
                    acmeDnsWebcon: response.acmeDnsWebcon,
                    acmePortWebcon: response.acmePortWebcon,
                    tosAcceptWebcon: response.tosAcceptWebcon
                });
                this.subscribeToFormChanges();
            });
    }

    subscribeToFormChanges(): void {
        this.settingsForm.valueChanges.subscribe(_ => {
            this.saved = false;
        });
    }

    save(): void {
        if (this.settingsForm.valid) {
            this.settingsService.store(this.settingsForm.value)
                .subscribe(() => this.saved = true);
        }
    }
}
