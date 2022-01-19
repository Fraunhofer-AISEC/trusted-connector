import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { User } from './user';

@Injectable()
export class UserService {
  constructor(private readonly http: HttpClient) { }

  // get users
  public getUsers(): Observable<User[]> {
    return this.http.get<User[]>(environment.apiURL + '/user/list_users');
  }

  // create new user
  public createUser(user: User): Observable<string> {
      const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
      const body = JSON.stringify(user);
      return this.http.post(environment.apiURL + '/user/saveUser', body, {
        headers,
        responseType: 'text'
      });
    }

  // set password
  public setPassword(user: User): Observable<string> {
      const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
      const body = JSON.stringify(user);
      return this.http.post(environment.apiURL + '/user/setPassword', body, {
        headers,
        responseType: 'text'
      });
  }

  // delete user
  public deleteUser(alias: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(environment.apiURL + '/user/removeUser', alias, {
      headers,
      responseType: 'text'
    });
  }
}
