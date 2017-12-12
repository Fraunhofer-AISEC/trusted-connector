import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { ConnectionConfigurationService } from './connectionConfiguration.service';
import { Configuration } from './configuration.interface';
import { Settings } from './settings.interface';

@Component({
  selector: 'connection-configuration',
  templateUrl: './connectionConfiguration.component.html',
  styleUrls: ['./connectionConfiguration.component.css']
})

export class ConnectionConfigurationComponent implements OnInit {

  myState: String = new String('General configuration');

  temp: String = 'temp';

  models: Configuration[];
  model: Configuration = { 
    connection: 'General',
    settings: {
    integrityProtectionandVerification: '1',
    authentication: '1',
    serviceIsolation: '1',
    integrityProtectionVerificationScope: '1', 
    appExecutionResources: '1',
    dataUsageControlSupport: '1', 
    auditLogging: '1',
    localDataConfidentiality: '1' }
  };

  constructor(private titleService: Title, private connectionConfService: ConnectionConfigurationService) {
       this.titleService.setTitle('Connections Configuration');
  }

  public ngOnInit(): void {
    let sets =  this.connectionConfService.getAllConfiguration().subscribe(val => {
     this.models = val;
     this.models.forEach(element => {
       if(element.connection == "General Configuration") {
        this.model = element;
       }
     });
    });
  }

  onChange(event){
    for( var _i = 0; _i < this.models.length; _i++) {
      if(this.models[_i].connection === this.model.connection) {
        this.models[_i] = this.model;
        break;
      }
    }
    
    for( var _i = 0; _i < this.models.length; _i++) {
      this.temp = this.models[_i].connection;
     // console.log("second loop - " + this.temp  + " -- " + this.myState + " ---- " + event.target.value);
      if( this.temp ===  event.target.value) {
       // console.log("okkkkk");
        this.model = this.models[_i];
       break;
      }
    }
  }

  save() {
    
    //Update current configuration
    for( var _i = 0; _i < this.models.length; _i++) {
            if(this.models[_i].connection === this.model.connection) {
              this.models[_i] = this.model;
              break;
            }
    }
    
    // Call connection service API to store configurations
    for( var _i = 0; _i < this.models.length; _i++) {
        let storePromise = this.connectionConfService.store(this.models[_i]);
        storePromise.subscribe(
            () => {
                console.log("saved configuration ");
            },
            err => console.log("Did not save configuration: " + this.models[_i].connection)
        );
    }
  }
}