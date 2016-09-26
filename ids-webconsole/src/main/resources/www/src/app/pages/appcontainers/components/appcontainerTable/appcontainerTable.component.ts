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

  settings = {
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
      name: {
        title: 'Name',
        type: 'string'
      },
      size: {
        title: 'Size',
        type: 'number'
      }
      
    }
  };

  source: LocalDataSource = new LocalDataSource();

  constructor(protected service: AppContainerService) {
    this.service.getData().then((data) => {
      this.source.load(data);
    });
  }

  onDeleteConfirm(event): void {
    if (window.confirm('Are you sure you want to delete?')) {
      event.confirm.resolve();
    } else {
      event.confirm.reject();
    }
  }
}
