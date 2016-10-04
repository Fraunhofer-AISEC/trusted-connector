import {Component} from '@angular/core';
import {Http} from '@angular/http';
import {platformBrowserDynamic} from "@angular/platform-browser-dynamic";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule} from "@angular/forms";
// TODO collect all endpoint definitions in one file:
//import {CREATE_USER_ENDPOINT} from '../shared/api';


@Component({
  selector: 'inline-form',
  template: require('./inlineForm.html'),
})
export class InlineForm {

  // This object is birectionally updated by the web form and the REST API
  settings: Object = {};
 
  constructor(private http: Http) {}
  
   onSubmit() {
    this.http.post("cxf/api/settings", this.settings)
      .subscribe(
        data => alert('Saved'),
        error => alert(error)
      );
  }
}
