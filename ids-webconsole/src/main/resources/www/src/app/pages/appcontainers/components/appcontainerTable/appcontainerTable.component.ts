import {Component, ViewEncapsulation} from '@angular/core';

import { AppContainerService } from './appcontainerTable.service';
import { LocalDataSource } from 'ng2-smart-table';

@Component({
  selector: 'basic-tables',
  encapsulation: ViewEncapsulation.None,
  styles: [require('./appcontainerTable.scss')],
  template: require('./appcontainerTable.html')
})
export class AppContainerTable {

  query: string = '';

  /**
   * Configuration of smart table
   */
  settings = {
    actions: {
      columnTitle: 'Actions',
      add: false,
      edit: false,
      delete: true
    },
    add: {
      addButtonContent: '<i class="ion-ios-plus-outline"></i>',
      createButtonContent: '<i class="ion-checkmark"></i>',
      cancelButtonContent: '<i class="ion-close"></i>',
    },
    edit: {
      editButtonContent: '<i class="ion-edit"></i>',
      saveButtonContent: '<i class="ion-checkmark"></i>',
      cancelButtonContent: '<i class="ion-close"></i>',
    },
    delete: {
      deleteButtonContent: '<i class="ion-trash-a"></i>',
      confirmDelete: true
    },
    columns: {
      id: {
        title: 'ID',
        type: 'number'
      },
      image: {
        title: 'Image',
        type: 'string'
      },
      names: {
        title: 'Name',
        type: 'string'
      },
      uptime: {
        title: 'Uptime',
        type: 'string'
      }
      
    }
  };

  /**
   * Source object holding entries of smart table.
   */
  source: LocalDataSource = new LocalDataSource();

  constructor(protected service: AppContainerService) {
    this.service.getData().then((data) => {
      this.source.load(data);
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
