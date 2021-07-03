import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { User } from './user';

@Injectable()
export class UserService {
  public userss: User[];
  constructor(private readonly http: HttpClient) { }

  // get Users

  public getUsers(): Observable<User[]> {
    return this.http.get<User[]>(environment.apiURL + '/user/list_users');
  }

  // create new User
  public createUser(user: User): Observable<string> {
      const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
      const body = JSON.stringify(user);
      return this.http.post(environment.apiURL + '/user/saveUser', body, {
        headers,
        responseType: 'text'
      });
    }

  // Set password
  /*
  public setPassword(user: User): Observable<string> {

  }
  */

  // delete user
  public deleteUser(alias: string): Observable<string> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(environment.apiURL + '/user/removeUser', alias, {
      headers,
      responseType: 'text'
    });
  }
}
