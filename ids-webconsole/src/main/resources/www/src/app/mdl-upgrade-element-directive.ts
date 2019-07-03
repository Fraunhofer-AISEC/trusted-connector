import { AfterViewChecked, Directive } from '@angular/core';
declare var componentHandler: {
    upgradeAllRegistered(): void;
};

@Directive({
    selector: '[appNgxMdl]'
})
export class MDLUpgradeElementDirective implements AfterViewChecked {
    public ngAfterViewChecked(): void {
        componentHandler.upgradeAllRegistered();
    }
}
