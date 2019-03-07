import { AfterViewInit, Component, Input } from '@angular/core';

import { App } from './app';
import { AppService } from './app.service';

declare var componentHandler: any;

@Component({
    selector: 'app-search-result-card',
    templateUrl: './app-search-result-card.component.html',
    styleUrls: ['./app-search-result-card.component.css']
})
export class AppSearchResultCardComponent implements AfterViewInit {
    @Input() app: App;

    constructor(private readonly appService: AppService) { }

    ngAfterViewInit(): void {
        setTimeout(() => {
            componentHandler.upgradeAllRegistered();
        }, 10);
    }

    onInstall(app: App): void {
        this.appService
            .installApp(app)
            .subscribe(success => success);
    }

    trackApps(index: number, item: App): string {
        return item.id;
    }

    trackCat(index: number, item: string): string {
        return item;
    }
}
