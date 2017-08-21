import {AfterViewChecked, AfterViewInit, Directive, ElementRef} from '@angular/core';
declare var componentHandler: any;

/**
 * Created to make MDL field validation and dirt check compatible with angular
 * Used in <input> and <textarea> tags
 */
@Directive({
    selector: '[mdl-textfield]'
})
export class MDLTextFieldDirective implements AfterViewChecked, AfterViewInit {

    constructor(private element: ElementRef) {
    }

    ngAfterViewInit() {
        console.log("After view init");
        componentHandler.upgradeAllRegistered();
    }

    ngAfterViewChecked() {
        let bla = this.element.nativeElement;
        let mdlField = this.element.nativeElement.MaterialTextfield;
        if(mdlField) {
            mdlField.checkDirty();
            mdlField.checkValidity();
        } else {
            console.log("Directive not applied to mdl textfield. Make sure to add directive to outer div.");
        }
    }
}