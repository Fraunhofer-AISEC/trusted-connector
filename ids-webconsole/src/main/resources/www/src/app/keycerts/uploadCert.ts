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
	   .mdl-button--file input {
		  cursor: pointer;
		  height: 100%;
		  right: 0;
		  opacity: 0;
		  position: absolute;
		  top: 0;
		  width: 300px;
		  z-index: 4;
		}
		.mdl-textfield--file .mdl-textfield__input {
		  box-sizing: border-box;
		  width: calc(100% - 32px);
		}
		.mdl-textfield--file .mdl-button--file {
		  right: 0;
		}
     `],

  template: `
   <dialog class="mdl-dialog">
		<h4 class="mdl-dialog__title">Install Certificate</h4>
		<div class="mdl-dialog__content">
			<!-- <input type="file" accept=".crt,.der,.cer" name="documents" (change)="onChange($event)"> -->

    <div class="mdl-textfield mdl-js-textfield mdl-textfield--file">
      <input class="mdl-textfield__input" placeholder="No file chosen" type="text" id="document.text" readonly />
      <div class="mdl-button mdl-button--icon mdl-button--file">
        <i class="material-icons">attach_file</i>
        <input type="file"  accept=".crt,.der,.cer" name="documents" id="documents" (change)="onChange($event)" />
      </div>
    </div>

			<div class="mdl-dialog__actions">
      			<button class="mdl-button" (click)="onUpload()">Upload</button>
      			<button class="mdl-button close" (click)="onCancel()">Cancel</button>
			</div>
      </div>
   </dialog>`
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
      this.dialog.close(true);
    }
  }

  onCancel() {
    this.dialog.close(false);
  }

  onChange(event: EventTarget) {
        let eventObj: MSInputMethodContext = <MSInputMethodContext> event;
        let target: HTMLInputElement = <HTMLInputElement> eventObj.target;
        let files: FileList = target.files;
        this.file = files[0];
        (<HTMLInputElement>document.getElementById("document.text")).value=this.file.name;
    }
}
