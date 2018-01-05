import { Injectable } from '@angular/core';
import { Headers, Http, Response, RequestOptions } from '@angular/http';
import {Observable} from 'rxjs/Observable';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/catch';

import { Policy } from './policy.interface';

import { environment } from '../../environments/environment';

@Injectable()
export class PolicyService {
    constructor(private http: Http) { }

    getPolicies() {
        return this.http.get(environment.apiURL + '/policies/list')
            .map(response => {
                return response.json() as string[];
            });
    }

    // Installs a LUCON policy through the PAP
    install(policy: Policy, policyFile: any) {
        let headers = new Headers({ 'Content-Type': 'multipart/form-data' });
        let options = new RequestOptions({ headers: headers });
        var model = new FormData();
        model.append("policy_name", policy.policy_name);
        model.append("policy_description", policy.policy_description);
        model.append("policy_file", policyFile);

        let result = this.http.post(environment.apiURL + '/policies/install', model, options)
            .catch((error: any) => Observable.throw(error || 'Server error'));
        return result;
    }
}
