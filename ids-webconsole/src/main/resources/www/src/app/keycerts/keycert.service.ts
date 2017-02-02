import { Injectable } from '@angular/core';

import 'rxjs/add/operator/map';

import { Certificate } from './certificate';

import { environment } from '../../environments/environment';

@Injectable()
export class CertificateService {
  local_certificates: Certificate[] = [
    {id: "11", title: 'Certification one', description: "It's first certification"},
    {id: "12", title: 'Certification two', description: "It's second certification"},
    {id: "13", title: 'Certification three', description: "It's third certification"},
  ];

  getCertificates(): Certificate[] {
    return this.local_certificates;
  }
}
