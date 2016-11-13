import {Component, ViewEncapsulation} from '@angular/core';

import { AppContainerMasonryService } from './appcontainerMasonry.service';
import { LocalDataSource } from 'ng2-smart-table';
 
 
@Component({
   selector: 'app-container-masonry',
   encapsulation: ViewEncapsulation.None,
   styles: [require('./appcontainerMasonry.scss')],
   //template: require('./appcontainerMasonry.html'),
   template: require('./appcontainerMasonry.html') 
   }) 
export class AppContainerMasonry {

  /**
   * Source object holding entries of smart table.
   */
  apps: JSON;

  constructor(protected service: AppContainerMasonryService) {
    this.service.getData().then((data) => {
      this.apps = data;
    });
  }

  onDeleteConfirm(event): void {
    if (window.confirm('Are you sure you want to delete this app? You will not be able to restore it!')) {
      event.confirm.resolve();
    } else {
      event.confirm.reject();
    }
  }
}
