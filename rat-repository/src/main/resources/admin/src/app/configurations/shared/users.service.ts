import { Injectable } from '@angular/core';
import {
  Headers,
  Http, 
  Request, 
  RequestOptions,
  RequestMethod
} from '@angular/http';


import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/catch';
import { Observable } from 'rxjs/Observable';

@Injectable()
export class UsersService {

  private url: string = "http://localhost:31337/json/configurations";

  constructor(private http: Http) { }

  getUsers(){
    return this.http.get(this.url)
      .map(res => res.json());
  }

  getUser(id){
    return this.http.get(this.getUserUrl(id))
      .map(res => res.json());
  }

  addUser(user){
    return this.http.post(this.url, JSON.stringify(user))
      .map(res => res.json());
  }

  updateUser(user){
    return this.http.put(this.getUserUrl(user.id), JSON.stringify(user))
      .map(res => res.json());
  }

  deleteUser(id){
    var reqOptions = new RequestOptions({ 
        method: RequestMethod.Delete,
        headers: new Headers({'Access-Control-Allow-Origin':'*'}),
    });
    return this.http.delete(this.getUserUrl(id), reqOptions)
      .map(res => res.json());
  }

  private getUserUrl(id){
    return this.url + "/" + id;
  }
}
