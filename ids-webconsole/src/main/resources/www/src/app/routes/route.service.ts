import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/map';

import { Route } from './route';

import { environment } from '../../environments/environment';

@Injectable()
export class RouteService {
  constructor(private http: Http) { }

  getRoute(route: any) {
    return this.http.get(environment.apiURL + '/routes/get/' + route)
               .map(response => {
                 return response.json() as Route;
               });
  }

  getTotalMessages() {
    return this.http.get(environment.apiURL + '/routes/list/')
               .map(response => {
                 let routes = response.json() as Route[];
                 let messages = 0;
                 routes.forEach((route) => messages += +route.messages);

                 return messages;
               });
  }

  getRoutes() {
    return this.http.get(environment.apiURL + '/routes/list/')
               .map(response => {
                 return response.json() as Route[];
               });
  }

  stopRoute(routeId: string) {
      // -------------------------------------------------------------------------------
      // Hardcoded just for demonstration
      if(routeId == "OPC-UA: Read Engine RPM (Trusted)") {
        this.http.get('http://iot-connector1.netsec.aisec.fraunhofer.de/led/1/power/false')
          .map(response => {
            return {};
          });
      } else if(routeId == "OPC-UA: Read Engine Current (Untrusted)") {
        this.http.get('http://iot-connector1.netsec.aisec.fraunhofer.de/led/2/power/false')
          .map(response => {
            return {};
          });
      }
      // -------------------------------------------------------------------------------

      return this.http.get(environment.apiURL + '/routes/stoproute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }

  startRoute(routeId: string) {
    // -------------------------------------------------------------------------------
    // Hardcoded just for demonstration
    if(routeId == "OPC-UA: Read Engine RPM (Trusted)") {
      this.http.get('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/1/power/true')
        .map(response => {
          return {};
        });
    } else if(routeId == "OPC-UA: Read Engine Current (Untrusted)") {
      this.http.get('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/2/power/true')
        .map(response => {
          return {};
        });
    }
    // -------------------------------------------------------------------------------

      //Start Camel route
      return this.http.get(environment.apiURL + '/routes/startroute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }
}
