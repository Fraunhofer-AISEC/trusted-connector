import { Component, OnInit } from '@angular/core';

import { ConfirmService } from './confirm.service';

const KEY_ESC = 27;

@Component({
    selector: 'modal-confirm',
    templateUrl: './confirm.component.html',
    styleUrls: ['./confirm.component.css']
})
export class ConfirmComponent implements OnInit {

    title: string;
    message: string;
    okText: string;
    cancelText: string;

    private readonly _defaults = {
        title: 'Confirmation',
        message: 'Do you want to cancel your changes?',
        cancelText: 'Cancel',
        okText: 'OK'
    };

    private _confirmElement: any;
    private _cancelButton: any;
    private _okButton: any;

    constructor(confirmService: ConfirmService) {
        confirmService.activate = this.activate.bind(this);
    }

    ngOnInit(): void {
        this._confirmElement = document.getElementById('confirmationModal');
        this._cancelButton = document.getElementById('cancelButton');
        this._okButton = document.getElementById('okButton');
    }

    _setLabels(message = this._defaults.message, title = this._defaults.title): void {
        this.title = title;
        this.message = message;
        this.okText = this._defaults.okText;
        this.cancelText = this._defaults.cancelText;
    }

    async activate(message = this._defaults.message, title = this._defaults.title): Promise<boolean> {
        this._setLabels(message, title);

        return new Promise<boolean>(resolve => {
            this._show(resolve);
        });
    }

    private _show(resolve: ((b: boolean) => any)): void {
        document.onkeyup = undefined;

        const negativeOnClick = (e: any) => resolve(false);
        const positiveOnClick = (e: any) => resolve(true);

        if (!this._confirmElement || !this._cancelButton || !this._okButton) {
            return;
        }

        this._confirmElement.style.opacity = 0;
        this._confirmElement.style.zIndex = 9999;

        this._cancelButton.onclick = ((e: any) => {
            e.preventDefault();
            if (!negativeOnClick(e)) {
                this._hideDialog();
            }
        });

        this._okButton.onclick = ((e: any) => {
            e.preventDefault();
            if (!positiveOnClick(e)) {
                this._hideDialog();
            }
        });

        this._confirmElement.onclick = () => {
            this._hideDialog();

            return negativeOnClick(undefined);
        };

        document.onkeyup = (e: any) => {
            if (e.which === KEY_ESC) {
                this._hideDialog();

                return negativeOnClick(undefined);
            }
        };

        this._confirmElement.style.opacity = 1;
    }

    private _hideDialog(): void {
        document.onkeyup = undefined;
        this._confirmElement.style.opacity = 0;
        window.setTimeout(() => this._confirmElement.style.zIndex = -1, 400);
    }
}
