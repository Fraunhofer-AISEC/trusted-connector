import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../environments/environment';

import { Policy } from './policy.interface';

@Injectable()
export class PolicyService {
    constructor(private readonly http: HttpClient) { }

    getPolicies(): Observable<Array<string>> {
        return this.http.get<Array<string>>(environment.apiURL + '/policies/list');
    }

    // Installs a LUCON policy through the PAP
    install(policy: Policy, policyFile: any): Observable<string> {
        const headers = new HttpHeaders({ 'Content-Type': 'multipart/form-data' });
        const model = new FormData();
        model.append('policy_name', policy.policy_name);
        model.append('policy_description', policy.policy_description);
        model.append('policy_file', policyFile);

        return this.http.post<string>(environment.apiURL + '/policies/install', model, { headers })
            .pipe(catchError((error: any) => throwError(new Error(error || 'Server error'))));
    }
}
