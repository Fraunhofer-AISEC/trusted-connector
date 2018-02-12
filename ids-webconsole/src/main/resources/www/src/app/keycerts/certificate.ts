export class Certificate {
  alias: string;
  file: string;
  certificate: string;
  subjectDistinguishedName: string;
  issuerDistinguishedName: string;
  subjectAltnames: Array<string>;
  subjectCN: string;
  subjectOU: string;
  subjectO: string;
  subjectL: string;
  subjectS: string;
  subjectC: string;
}
