import 'zone.js';
import 'reflect-metadata';
import 'jsog';
import 'intl';
import 'intl/locale-data/jsonp/en.js';
import 'cal-heatmap';

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app.module';

import {enableProdMode} from '@angular/core';
enableProdMode();

platformBrowserDynamic().bootstrapModule(AppModule);
