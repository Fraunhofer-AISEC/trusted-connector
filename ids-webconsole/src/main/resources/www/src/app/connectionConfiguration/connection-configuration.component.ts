import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { ConnectionConfigurationService } from './connection-configuration.service';
import { Configuration } from './configuration';

@Component({
  selector: 'connection-configuration',
  templateUrl: './connection-configuration.component.html',
  styleUrls: ['./connection-configuration.component.css']
})
export class ConnectionConfigurationComponent implements OnInit {
  public models: Configuration[];
  private _model?: Configuration;
  private _selectedIndex = 0;

  constructor(private titleService: Title, private connectionConfService: ConnectionConfigurationService) {
    this.titleService.setTitle('Connections Configuration');
  }

  public ngOnInit(): void {
    let sets = this.connectionConfService.getAllConfiguration().subscribe(val => {
      this.models = val;
      for (let i = 0; i < this.models.length; i++) {
        if (this.models[i].connection === 'General Configuration') {
          this.selectedIndex = i;
        }
      }
    });
  }

  get selectedIndex() {
    return this._selectedIndex;
  }

  set selectedIndex(selectedIndex: number) {
    this._selectedIndex = selectedIndex;
    this._model = this.models[selectedIndex];
  }

  get model() {
    return this._model;
  }

  onChange(event) {
    console.log(this.selectedIndex);
    this.selectedIndex = event.target.value;
  }

  save() {
    // Call connection service API to store configurations
    this.connectionConfService.storeConfiguration(this.model).subscribe(
      () => {
        console.log('Saved configuration ' + this.model.connection);
        this.model.dirty = false;
      },
      err => console.log('Error occured while saving configuration ' + this.model.connection + ': ', err)
    );
  }

}
