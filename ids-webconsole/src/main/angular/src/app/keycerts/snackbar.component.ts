import { Component, Input } from '@angular/core';

@Component({
    selector: 'snackbar',
    templateUrl: './snackbar.component.html',
    styleUrl: './snackbar.component.css'
})
export class SnackbarComponent {
    @Input() title: string = null;
    @Input() subtitle: string = null;
    @Input() visible: boolean = false;
    @Input() onDismiss: ()=>void = null;

    invokeOnDismiss() {
        if (this.onDismiss !== null) {
            this.onDismiss()
        } else {
            this.visible = false;
        }
    }
}