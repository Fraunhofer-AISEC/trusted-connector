import { Component, Input, OnInit } from '@angular/core';

import { App } from './app';
import { AppService } from './app.service';
import { PortDef } from './app.port.def';

@Component({
    selector: 'app-card',
    templateUrl: './app-card.component.html'
})
export class AppCardComponent implements OnInit {
    @Input() app: App;
    statusIcon: string;
    statusColor: string;
    private portDefs: Array<PortDef>;

    constructor(private appService: AppService) { }

    get ports(): Array<PortDef> {
        if (!this.portDefs) {
            this.portDefs = this.app.ports
                .map(list => list.split(',')
                    .map(portDef => new PortDef(portDef.trim())))
                .reduce((acc, x) => acc.concat(x), []);
        }

        return this.portDefs;
    }

    trackPorts(_: number, item: PortDef): string {
        return item.text;
    }

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
}
