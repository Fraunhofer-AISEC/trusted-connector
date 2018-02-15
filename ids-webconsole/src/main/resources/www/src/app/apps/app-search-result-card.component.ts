import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { App, DockerHubApp } from './app';
import { AppService } from './app.service';

@Component({
    selector: 'app-search-result-card',
    templateUrl: './app-search-result-card.component.html'
})
export class AppSearchResultCardComponent implements OnInit {
    @Input() app: DockerHubApp;

    constructor(private appService: AppService) { }
    ngOnInit(): void {
    }
}
