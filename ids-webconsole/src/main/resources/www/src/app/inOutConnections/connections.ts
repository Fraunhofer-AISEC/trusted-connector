export class IncomingConnection {
  endpointIdentifier: string;
  attestationResult: string;
}

export class OutgoingConnection {
  endpointIdentifier: string;
  lastProtocolState: string;
  remoteAuthentication: string;
  remoteIdentity: string;
}
