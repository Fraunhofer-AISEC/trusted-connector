import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Headers, Http, RequestOptions, Response } from '@angular/http';
import { CanDeactivate } from '@angular/router';
import { SettingsService } from './settings.service';
import { Settings } from './settings.interface';
import { environment } from '../../environments/environment';

@Component({
    selector: 'my-app',
    templateUrl: './ids.component.html',
    providers: [SettingsService]
})
export class IdsComponent implements OnInit, CanDeactivate<IdsComponent> {
    myForm: FormGroup;
    data: Settings;
    submitted: boolean;
    saved: boolean;
    events: Array<any> = [];

    constructor(private _fb: FormBuilder, private _http: Http, private _settingsService: SettingsService) {
        this.saved = true;
    }

    canDeactivate(target: IdsComponent): boolean {
        return target.saved; // false stops navigation, true continue navigation
    }

    ngOnInit(): void {
        // the short way
        this.myForm = this._fb.group({
            broker_url: ['', [Validators.required as any, Validators.minLength(5) as any]],
            ttp_host: ['', [Validators.required as any, Validators.minLength(3) as any]],
            ttp_port: ['', [Validators.required as any, Validators.maxLength(5) as any]],
            announce_by_gossip: [],
            acme_url: ['', [Validators.required as any, Validators.maxLength(5) as any]]
        });

        // subscribe to form changes
        this.subcribeToFormChanges();

        this._settingsService.getSettings()
            .subscribe(
                response => {
                    this.data = response;
                    (this.myForm.controls['broker_url'] as FormControl).setValue(this.data.broker_url, { onlySelf: true });
                    (this.myForm.controls['ttp_host'] as FormControl).setValue(this.data.ttp_host, { onlySelf: true });
                    (this.myForm.controls['ttp_port'] as FormControl).setValue(this.data.ttp_port, { onlySelf: true });
                    (this.myForm.controls['acme_url'] as FormControl).setValue(this.data.acme_url, { onlySelf: true });
                    (this.myForm.controls['announce_by_gossip'] as FormControl).setValue(this.data.announce_by_gossip, { onlySelf: true });
                }
            );

    }

    subcribeToFormChanges(): void {
        const myFormStatusChanges$ = this.myForm.statusChanges;
        const myFormValueChanges$ = this.myForm.valueChanges;

        myFormStatusChanges$.subscribe(x => this.events.push({ event: 'STATUS_CHANGED', object: x }));
        myFormValueChanges$.subscribe(x => {
            this.saved = false;
            this.events.push({ event: 'VALUE_CHANGED', object: x });
        });
    }

    save(model: Settings, isValid: boolean): void {
        this.submitted = true;
        // console.log(model, isValid);

        // Call REST POST to store settings
        const storePromise = this._settingsService.store(model);
        storePromise.subscribe(
            () => {
                // If saved successfully, user may leave the route (=saved=true)
                this.saved = true;
            }
            // err => console.log('Did not save form ' + err.json().message)
        );

    }
}
