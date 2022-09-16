import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { User } from './user.interface';

@Injectable()
export class UserService {
    constructor(private readonly http: HttpClient) {
    }

    // get users
    public getUsers(): Observable<string[]> {
        return this.http.get<string[]>(environment.apiURL + '/user/list_user_names');
    }

    // create new user
    public async createUser(user: User): Promise<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});
        return this.http.post(environment.apiURL + '/user/saveUser', user, {
            headers,
            responseType: 'text'
        }).toPromise();
    }

    // set password
    public async setPassword(user: string, oldPW: string, newPW: string): Promise<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});
        return this.http.post(environment.apiURL + '/user/setPassword', {
            username: user,
            oldPassword: oldPW,
            newPassword: newPW
        }, {
            headers,
            responseType: 'text'
        }).toPromise();
    }

    // delete user
    public async deleteUser(user: string): Promise<string> {
        const headers = new HttpHeaders({'Content-Type': 'application/json'});
        return this.http.delete(environment.apiURL + '/user/removeUser/' + encodeURIComponent(user), {
            headers,
            responseType: 'text'
        }).toPromise();
    }
}
