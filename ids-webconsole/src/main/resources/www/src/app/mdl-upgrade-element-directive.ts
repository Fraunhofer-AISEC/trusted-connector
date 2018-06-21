import { AfterViewChecked, Directive } from '@angular/core';
declare var componentHandler: {
    upgradeAllRegistered(): void;
};

@Directive({
    selector: '[ngxMdl]'
})
export class MDLUpgradeElementDirective implements AfterViewChecked {
    ngAfterViewChecked(): void {
        componentHandler.upgradeAllRegistered();
    }
}
