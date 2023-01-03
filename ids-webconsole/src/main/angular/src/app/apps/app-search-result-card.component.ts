import { AfterViewInit, Component, Input } from '@angular/core';

import { App } from './app';
import { AppService } from './app.service';

declare let componentHandler: any;

@Component({
    selector: 'app-search-result-card',
    templateUrl: './app-search-result-card.component.html',
    styleUrls: ['./app-search-result-card.component.css']
})
export class AppSearchResultCardComponent implements AfterViewInit {
    @Input() public app: App;

    constructor(private readonly appService: AppService) { }

    public ngAfterViewInit(): void {
        setTimeout(() => {
            componentHandler.upgradeAllRegistered();
        }, 10);
    }

    public onInstall(app: App): void {
        this.appService
            .installApp(app)
            .subscribe();
    }

    public trackApps(index: number, item: App): string {
        return item.id;
    }

    public trackCat(index: number, item: string): string {
        return item;
    }
}
