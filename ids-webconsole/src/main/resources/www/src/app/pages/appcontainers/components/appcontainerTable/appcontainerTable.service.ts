import {Injectable} from '@angular/core';
import { Component, CORE_DIRECTIVES } from 'angular2/angular2';
import { Task } from '../datatypes/task';
import { Http } from '@angular/http';
import { AppContainer } from '../../appcontainers.component';


@Injectable()
export class AppContainerService {

  constructor(public http: Http) {
    console.log('AppContainer Service created.', http);
  }
   
  getData(): Promise<any> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        this.getTasks();
      }, 2000);
    });
  }
  
  getTasks() {
    // return an observable
    return this.http.get('/api/v1/containers')
    .map( (responseData) => {
      return responseData.json();
    })
    .map((acs: Array<any>) => {
      let result:Array<AppContainer> = [];
      if (acs) {
        acs.forEach((ac) => {
          // TOOD Create proper AppContainer objects from JSON results here
          result.push(new AppContainer("id", "name", "image", 23.5));
        });
      }
      return result;
    });
  }
}
