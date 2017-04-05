import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';
import { Headers, Http, Response, RequestOptions } from '@angular/http';
import { CanDeactivate } from '@angular/router';
import { Settings } from './settings.interface';
import { environment } from '../../environments/environment';

@Component({
  selector: 'my-app',
  templateUrl: './ids.component.html',
})
export class IdsComponent implements OnInit, CanDeactivate<IdsComponent> {
    public myForm: FormGroup;
    public submitted: boolean;
    public saved: boolean;
    public events: any[] = [];

    constructor(private _fb: FormBuilder, private _http: Http) { 
    	this.saved = true;
    }
    
    canDeactivate(target: IdsComponent){
    	return target.saved; // false stops navigation, true continue navigation
  	}

    ngOnInit() {
        // the long way
        // this.myForm = new FormGroup({
        //     name: new FormControl('', [<any>Validators.required, <any>Validators.minLength(5)]),
        //     address: new FormGroup({
        //         address1: new FormControl('', <any>Validators.required),
        //         postcode: new FormControl('8000')
        //     })
        // });

        // the short way
        this.myForm = this._fb.group({
            broker_url: ['', [<any>Validators.required, <any>Validators.minLength(5)]],
            ttp_address: ['', [<any>Validators.required, <any>Validators.minLength(5)]],
            
            // This is how to deal with form groups:
            //ttp_address: this._fb.group({
            //    street: ['', <any>Validators.required],
            //    postcode: ['8000']
            //})
        });

        // subscribe to form changes  
        this.subcribeToFormChanges();
                
        // This is how to update the whole form model
        // const people = {
        // 	name: 'Jane',
        // 	address: {
        // 		street: 'High street',
        // 		postcode: '94043'
        // 	}
        // };
        // (<FormGroup>this.myForm).setValue(people, { onlySelf: true });

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
        
    	let headers = new Headers({ 'Content-Type': 'application/json' });
    	let options = new RequestOptions({ headers: headers });

         // Call REST POST to store settings
         let result = this._http.post(environment.apiURL + '/config/set', model, options)
               .subscribe(
			      () => {
			        // If saved successfully, user may leave the route (=saved=true)
			        this.saved = true;
			      },
			      err => console.log("Did not save form " + err.json().message)
			    );
    }
}