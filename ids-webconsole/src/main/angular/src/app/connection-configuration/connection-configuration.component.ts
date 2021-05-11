import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Configuration } from './configuration';
import { ConnectionConfigurationService } from './connection-configuration.service';

@Component({
  selector: 'connection-configuration',
  templateUrl: './connection-configuration.component.html',
  styleUrls: ['./connection-configuration.component.css']
})
export class ConnectionConfigurationComponent implements OnInit {
  public models: Configuration[];
  private _model?: Configuration;
  private _selectedIndex = 0;

  constructor(private readonly titleService: Title, private readonly connectionConfService: ConnectionConfigurationService) {
    this.titleService.setTitle('Security Requirements');
  }

  public ngOnInit(): void {
    this.connectionConfService.getAllConfiguration()
      .subscribe(val => {
        this.models = val;
        for (let i = 0; i < this.models.length; i++) {
          if (this.models[i].connection === 'General Configuration') {
            this.selectedIndex = i;
          }
        }
      });
  }

  public trackModels(_index: number, item: Configuration): string {
    return item.connection;
  }

  get selectedIndex(): number {
    return this._selectedIndex;
  }

  set selectedIndex(selectedIndex: number) {
    this._selectedIndex = selectedIndex;
    this._model = this.models[selectedIndex];
  }

  get model(): Configuration {
    return this._model;
  }

  public save(): void {
    // Call connection service API to store configurations
    this.connectionConfService.storeConfiguration(this.model)
      .subscribe(
        () => {
          // console.log('Saved configuration ' + this.model.connection);
          this.model.dirty = false;
        }
        // err => console.log('Error occured while saving configuration ' + this.model.connection + ': ', err)
      );
  }

}
