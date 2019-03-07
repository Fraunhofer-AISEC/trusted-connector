import { Component, Input, OnInit } from '@angular/core';

import { App } from './app';
import { AppService } from './app.service';
import { AppsComponent } from './apps.component';

@Component({
    selector: 'app-card',
    templateUrl: './app-card.component.html'
})
export class AppCardComponent implements OnInit {
    @Input() app: App;
    statusIcon: string;
    statusColor: string;

    constructor(private appService: AppService, private appsComponent: AppsComponent) {}
    ngOnInit(): void {
        if (this.app.status.indexOf('Up') >= 0) {
            this.statusIcon = 'stop';
            this.statusColor = '';
        } else {
            this.statusIcon = 'play_arrow';
            this.statusColor = 'card-dark';
        }
    }

    onToggle(containerId: string): void {
        if (this.statusIcon === 'play_arrow') {
            this.statusIcon = 'stop';
            this.statusColor = '';
            this.appService.startApp(containerId)
              .subscribe(result => undefined);
            this.app.status = 'Up 1 seconds ago';
        } else {
            this.statusIcon = 'play_arrow';
            this.statusColor = 'card-dark';
            this.appService.stopApp(containerId)
              .subscribe(result => undefined);
            this.app.status = 'Exited(0) 1 seconds ago';
        }
    }

    onDeleteBtnClick(containerId: string): void {
      this.appService.wipeApp(containerId)
        .subscribe(result => undefined);
      const index = this.appsComponent.apps.indexOf(this.app);
      this.appsComponent.apps.splice(index, 1) ;
    }

    trackPorts(index: number, item: string): string {
        return item;
    }
}
