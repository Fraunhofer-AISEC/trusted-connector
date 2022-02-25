import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { User } from './user.interface';

@Injectable()
export class UserService {
  constructor(private readonly http: HttpClient) { }

  // get users
  public getUsers(): Observable<string[]> {
    return this.http.get<string[]>(environment.apiURL + '/user/list_user_names');
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
  public setPassword(user: string, oldPW: string, newPW: string): Observable<string> {
      const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
      const s = '{\"username\":\"'+user+'\",\"oldPassword\":\"'+oldPW+'\",\"newPassword\":\"'+newPW+'\"}';
      return this.http.post(environment.apiURL + '/user/setPassword', s, {
        headers,
        responseType: 'text'
      });
  }

  // delete user
  public deleteUser(user: string): Observable<string> {
    console.log('delete:'+ environment.apiURL + '/user/removeUser/' + user);
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.delete(environment.apiURL + '/user/removeUser/'+  user, {
      headers,
      responseType: 'text'
    });
  }
}
