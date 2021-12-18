import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Route } from './route';
import { RouteService } from './route.service';

@Component({
    selector: 'route-list',
    templateUrl: './routes.component.html',
    styleUrls: ['./routes.component.css']
})

export class RoutesComponent implements OnInit {
    @Output() public readonly changeTitle = new EventEmitter();
    public title = 'Current Routes';
    public routes: Route[];
    public selectedRoute: Route;

    constructor(private readonly titleService: Title, private readonly routeService: RouteService) {
        this.titleService.setTitle('Message Routes');

        this.routeService.getRoutes()
            .subscribe(routes => {
                this.routes = routes;
            });
    }

    public ngOnInit(): void {
        this.changeTitle.emit('Camel Routes');
    }

    public trackRoutes(index: number, item: Route): string {
        return item.id;
    }

    public onSelect(route: Route): void {
        this.selectedRoute = route;
    }
}
