import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { SettingsService } from './settings.service';
import { Settings } from './settings.interface';
import { environment } from '../../environments/environment';

@Component({
    selector: 'my-app',
    templateUrl: './ids.component.html',
    styleUrls: ['./ids.component.css'],
    providers: [SettingsService]
})
export class IdsComponent implements OnInit {
    myForm: FormGroup;
    submitted: boolean;
    saved: boolean;
    // events: Array<any> = [];

    constructor(private settingsService: SettingsService) {
        this.saved = true;
    }

    canDeactivate(target: IdsComponent): boolean {
        return target.saved; // false stops navigation, true continue navigation
    }

    ngOnInit(): void {
        this.settingsService.getSettings()
            .subscribe(response => {
                this.myForm = new FormGroup({
                    brokerUrl: new FormControl(response.brokerUrl),
                    ttpHost: new FormControl(response.ttpHost),
                    ttpPort: new FormControl(response.ttpPort),
                    acmeServerWebcon: new FormControl(response.acmeServerWebcon),
                    acmeDnsWebcon: new FormControl(response.acmeDnsWebcon),
                    acmePortWebcon: new FormControl(response.acmePortWebcon)
                });
                // subscribe to form changes
                this.subscribeToFormChanges();
            });
    }

    subscribeToFormChanges(): void {
        // this.myForm.statusChanges.subscribe(x => this.events.push({ event: 'STATUS_CHANGED', object: x }));
        this.myForm.valueChanges.subscribe(x => {
            this.saved = false;
            // this.events.push({ event: 'VALUE_CHANGED', object: x });
            // console.log(this.myForm.controls.brokerUrl.valid, this.myForm.controls.brokerUrl.pristine,
            //     !this.myForm.controls.ttpHost.valid && this.myForm.controls.ttpHost.dirty);
        });
    }

    save(model: Settings, isValid: boolean): void {
        // console.log(model, isValid);
        if (isValid) {
            this.submitted = true;
            // Store settings
            this.settingsService.store(model)
                .subscribe(() => this.saved = true);
        }
    }
}
