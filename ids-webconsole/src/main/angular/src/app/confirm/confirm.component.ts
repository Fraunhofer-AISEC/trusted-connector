import { Component, OnInit } from '@angular/core';

import { ConfirmService } from './confirm.service';

const KEY_ESC = 27;

@Component({
    selector: 'modal-confirm',
    templateUrl: './confirm.component.html',
    styleUrls: ['./confirm.component.css']
})
export class ConfirmComponent implements OnInit {

    public title: string;
    public message: string;
    public okText: string;
    public cancelText: string;

    private readonly defaultTexts = {
        title: 'Confirmation',
        message: 'Do you want to cancel your changes?',
        okText: 'OK',
        cancelText: 'Cancel'
    };

    private confirmElement: any;
    private cancelButton: any;
    private okButton: any;

    constructor(confirmService: ConfirmService) {
        confirmService.activate = this.activate.bind(this);
    }

    public ngOnInit(): void {
        this.confirmElement = document.getElementById('confirmationModal');
        this.cancelButton = document.getElementById('cancelButton');
        this.okButton = document.getElementById('okButton');
    }

    public setLabels(message: string = this.defaultTexts.message, title: string = this.defaultTexts.title): void {
        this.title = title;
        this.message = message;
        this.okText = this.defaultTexts.okText;
        this.cancelText = this.defaultTexts.cancelText;
    }

    public async activate(message: string = this.defaultTexts.message, title: string = this.defaultTexts.title): Promise<boolean> {
        this.setLabels(message, title);

        return new Promise<boolean>(resolve => {
            this.show(resolve);
        });
    }

    private show(resolve: ((b: boolean) => any)): void {
        document.onkeyup = undefined;

        const negativeOnClick = (_e: any) => resolve(false);
        const positiveOnClick = (_e: any) => resolve(true);

        if (!this.confirmElement || !this.cancelButton || !this.okButton) {
            return;
        }

        this.confirmElement.style.opacity = 0;
        this.confirmElement.style.zIndex = 9999;

        this.cancelButton.onclick = ((e: any) => {
            e.preventDefault();
            if (!negativeOnClick(e)) {
                this.hideDialog();
            }
        });

        this.okButton.onclick = ((e: any) => {
            e.preventDefault();
            if (!positiveOnClick(e)) {
                this.hideDialog();
            }
        });

        this.confirmElement.onclick = () => {
            this.hideDialog();

            return negativeOnClick(undefined);
        };

        document.onkeyup = (e: any) => {
            if (e.which === KEY_ESC) {
                this.hideDialog();

                return negativeOnClick(undefined);
            }
        };

        this.confirmElement.style.opacity = 1;
    }

    private hideDialog(): void {
        document.onkeyup = undefined;
        this.confirmElement.style.opacity = 0;
        window.setTimeout(() => this.confirmElement.style.zIndex = -1, 400);
    }
}
