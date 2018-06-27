import { AfterViewInit, Component, Input } from '@angular/core';

import { App, DockerHubApp } from './app';
import { AppService } from './app.service';

declare var componentHandler: any;

@Component({
    selector: 'app-search-result-card',
    templateUrl: './app-search-result-card.component.html'
})
export class AppSearchResultCardComponent implements AfterViewInit {
    @Input() app: DockerHubApp;

    constructor(private appService: AppService) { }

    ngAfterViewInit(): void {
        setTimeout(() => {
            componentHandler.upgradeAllRegistered();
        }, 10);
    }

    onInstall(appName: string, tag: string): void {
        this.appService
            .installApp(appName, tag)
            .subscribe(success => success);
    }

    // Unused, just here to calm down the linter
    trackApps(index: number, item: App): string {
        return item.id;
    }
}
