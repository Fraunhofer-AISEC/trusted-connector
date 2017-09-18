export class IncommingConnection {
  endpointIdentifier: string;
  attestationResult: string;
}

export class OutgoingConnection {
  endpoint_identifier: string;
  lastProtocolState: string;
  remoteAuthentication: string;
  remoteIdentity: string;
}