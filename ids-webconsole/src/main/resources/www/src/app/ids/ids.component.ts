import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';
import { Headers, Http, Response, RequestOptions } from '@angular/http';
import { CanDeactivate } from '@angular/router';
import { SettingsService } from './settings.service';
import { Settings } from './settings.interface';
import { environment } from '../../environments/environment';

@Component({
  selector: 'my-app',
  templateUrl: './ids.component.html',
})
export class IdsComponent implements OnInit, CanDeactivate<IdsComponent> {
    public myForm: FormGroup;
	public data: Settings;
    public submitted: boolean;
    public saved: boolean;
    public events: any[] = [];

    constructor(private _fb: FormBuilder, private _http: Http, private _settingsService: SettingsService) {
    	this.saved = true;
    }

    canDeactivate(target: IdsComponent){
    	return target.saved; // false stops navigation, true continue navigation
  	}

    ngOnInit() {
        // the short way
        this.myForm = this._fb.group({
            broker_url: ['', [<any>Validators.required, <any>Validators.minLength(5)]],
            ttp_host: ['', [<any>Validators.required, <any>Validators.minLength(3)]],
            ttp_port: ['', [<any>Validators.required, <any>Validators.maxLength(5)]],
        });

        // subscribe to form changes
        this.subcribeToFormChanges();

		this._settingsService.getSettings().subscribe(
       		response => {
           		this.data = response;
           		(<FormControl>this.myForm.controls['broker_url']).setValue(this.data.broker_url, { onlySelf: true });
           		(<FormControl>this.myForm.controls['http_host']).setValue(this.data.http_host, { onlySelf: true });
           		(<FormControl>this.myForm.controls['http_port']).setValue(this.data.http_port, { onlySelf: true });
       		}
    	);

    }

    ngAfterViewInit() {
        // Update single value
        (<FormControl>this.myForm.controls['broker_url']).patchValue('ids://localhost', { onlySelf: true });
    }

    subcribeToFormChanges() {
        const myFormStatusChanges$ = this.myForm.statusChanges;
        const myFormValueChanges$ = this.myForm.valueChanges;

        myFormStatusChanges$.subscribe(x => this.events.push({ event: 'STATUS_CHANGED', object: x }));
        myFormValueChanges$.subscribe(x => { this.saved = false;
        									 this.events.push({ event: 'VALUE_CHANGED', object: x }) });
    }

    save(model: Settings, isValid: boolean) {
        this.submitted = true;
        console.log(model, isValid);

         // Call REST POST to store settings
		let storePromise = this._settingsService.store(model);
		storePromise.subscribe(
			      () => {
			        // If saved successfully, user may leave the route (=saved=true)
			        this.saved = true;
			      },
			      err => console.log("Did not save form " + err.json().message)
			    );

    }
}
