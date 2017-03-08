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
      if(routeId == "OPC-UA: Read Engine Power (Trusted)") {
        this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/1/power/false', {}).subscribe();
        this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/3/power/false', {}).subscribe();
        this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/4/power/false', {}).subscribe();
      } else if(routeId == "OPC-UA: Read Engine Power (Untrusted)") {
        this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/2/power/false', {}).subscribe();
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
    if(routeId == "OPC-UA: Read Engine Power (Trusted)") {
      this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/1/power/true', {}).subscribe();
      this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/3/power/true', {}).subscribe();
      this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/4/power/true', {}).subscribe();
    } else if(routeId == "OPC-UA: Read Engine Power (Untrusted)") {
      this.http.post('http://iot-connector1.netsec.aisec.fraunhofer.de:8282/led/2/power/true', {}).subscribe();
    }
    // -------------------------------------------------------------------------------

      //Start Camel route
      return this.http.get(environment.apiURL + '/routes/startroute/' + routeId)
                 .map(response => {
                   return response.json() as string;
                 });
  }
}
