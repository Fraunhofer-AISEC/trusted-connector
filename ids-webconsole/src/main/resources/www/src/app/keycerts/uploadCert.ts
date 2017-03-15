import { Component } from '@angular/core';

import { DialogRef, ModalComponent } from 'angular2-modal';
import { BSModalContext } from 'angular2-modal/plugins/bootstrap';

import { CertificateService } from './keycert.service';


export class CustomModalContext extends BSModalContext {
  public keystoreDestination: string;
}

@Component({
   selector: 'modal-content',
   styles: [`
         .custom-modal-container {
             padding: 15px;
         }

         .custom-modal-header {
             background-color: #219161;
             color: #fff;
             -webkit-box-shadow: 0px 3px 5px 0px rgba(0,0,0,0.75);
             -moz-box-shadow: 0px 3px 5px 0px rgba(0,0,0,0.75);
             box-shadow: 0px 3px 5px 0px rgba(0,0,0,0.75);
             margin-top: -15px;
             margin-bottom: 40px;
         }

         .custom-modal-button {
           margin-top: 15px;
         }
     `],

  template: `
   <div class="container-fluid custom-modal-container">
       <div class="row custom-modal-header">
           <div class="col-sm-12">
               <h1>Please select a certification</h1>
           </div>
       </div>
       <input type="file" class="form-control" accept=".crt,.der,.cer" name="documents" (change)="onChange($event)">

       <div class=" custom-modal-button">
          <div class="btn-toolbar btn-block pull-right">
          <button class="btn btn-primary pull-right" (click)="onUpload()">Upload</button>
          <button class="btn btn-primary pull-right" (click)="onCancel()">Cancel</button>
          </div>
      </div>
   </div>`
 })
export class FileWindow implements ModalComponent<CustomModalContext> {
  context: CustomModalContext;
  file: File;

  constructor(public dialog: DialogRef<CustomModalContext>, private certificateService: CertificateService) {
    this.context = dialog.context;
    this.context.size = 'lg';
  }

  ngOnInit(): void {
    this.file = null;
  }

  beforeDismiss(): boolean {
    return true;
  }

  beforeClose(): boolean {
    return true;
  }

  onUpload() {
    if (this.file !== null) {
      this.certificateService.uploadCert(this.file, this.context.keystoreDestination);
      this.file = null;
      this.dialog.close();
    }
  }

  onCancel() {
    this.dialog.close();
  }

  onChange(event: EventTarget) {
        let eventObj: MSInputMethodContext = <MSInputMethodContext> event;
        let target: HTMLInputElement = <HTMLInputElement> eventObj.target;
        let files: FileList = target.files;
        this.file = files[0];
    }
}
