import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../environments/environment';

import { Policy } from './policy.interface';

@Injectable()
export class PolicyService {
    constructor(private readonly http: HttpClient) { }

    public getPolicies(): Observable<string[]> {
        return this.http.get<string[]>(environment.apiURL + '/policies/list');
    }

    // Installs a LUCON policy through the PAP
    public install(policy: Policy, policyFile: any): Observable<string> {
        const headers = new HttpHeaders({ 'Content-Type': 'multipart/form-data' });
        const model = new FormData();
        model.append('policy_name', policy.policyName);
        model.append('policy_description', policy.policyDescription);
        model.append('policy_file', policyFile);

        return this.http.post<string>(environment.apiURL + '/policies/install', model, { headers })
            .pipe(catchError((error: any) => throwError(new Error(error || 'Server error'))));
    }
}
