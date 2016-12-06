//import 'intl';
//import 'intl/locale-data/jsonp/en.js';

import 'material-design-lite';

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app.module';

import {enableProdMode} from '@angular/core';
enableProdMode();

platformBrowserDynamic().bootstrapModule(AppModule);
