import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';

import { Route } from './route';
import { RouteService } from './route.service';
import { RouteMetrics } from './route-metrics';

@Component({
    selector: 'route-list',
    templateUrl: './routes.component.html',
})

export class RoutesComponent implements OnInit {
    title = 'Current Routes';
    routes: Route[];
    selectedRoute: Route;
    routemetrics: RouteMetrics = new RouteMetrics();

    @Output() changeTitle = new EventEmitter();

    constructor(private titleService: Title, private routeService: RouteService) {
        this.titleService.setTitle('Message Routes');

        this.routeService.getRoutes().subscribe(routes => {
            this.routes = routes;
        });
    }

    ngOnInit(): void {
        this.changeTitle.emit('Camel Routes');
        // Update route metrics every 1s
        Observable
            .timer(0, 1000)
            .subscribe(() => {
                this.routeService.getMetrics().subscribe(result => this.routemetrics = result);
            });
    }

    onSelect(route: Route): void {
        this.selectedRoute = route;
    }
}
