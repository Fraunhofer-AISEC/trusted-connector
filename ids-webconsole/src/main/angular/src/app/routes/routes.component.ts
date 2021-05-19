import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { interval } from 'rxjs';
import { mergeMap, takeWhile } from 'rxjs/operators';

import { Route } from './route';
import { RouteMetrics } from './route-metrics';
import { RouteService } from './route.service';

@Component({
    selector: 'route-list',
    templateUrl: './routes.component.html',
    styleUrls: ['./routes.component.css']
})

export class RoutesComponent implements OnInit, OnDestroy {
    @Output() public readonly changeTitle = new EventEmitter();
    public title = 'Current Routes';
    public routes: Route[];
    public selectedRoute: Route;
    public routemetrics: RouteMetrics = new RouteMetrics();

    private alive: boolean;

    constructor(private readonly titleService: Title, private readonly routeService: RouteService) {
        this.titleService.setTitle('Message Routes');

        this.routeService.getRoutes()
            .subscribe(routes => {
                this.routes = routes;
            });
    }

    public ngOnInit(): void {
        this.changeTitle.emit('Camel Routes');
        // Update route metrics every second
        interval(1000)
            .pipe(
                takeWhile(() => this.alive),
                mergeMap(() => this.routeService.getMetrics())
            )
            .subscribe(routeMetrics => this.routemetrics = routeMetrics);
        this.alive = true;
    }

    public ngOnDestroy(): void {
        this.alive = false;
    }

    public trackRoutes(index: number, item: Route): string {
        return item.id;
    }

    public onSelect(route: Route): void {
        this.selectedRoute = route;
    }
}
