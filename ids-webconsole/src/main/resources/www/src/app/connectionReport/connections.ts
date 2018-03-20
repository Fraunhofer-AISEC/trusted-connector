export class IncomingConnection {
  endpointIdentifier: string;
  attestationResult: string;
  connectionKey: string;
  remoteHostName: string;
}

export class OutgoingConnection {
  endpointIdentifier: string;
  remoteAuthentication: string;
  remoteIdentity: string;
  attestationResult: string;
}

export class Endpoint {
  endpointIdentifier: string;
  defaultProtocol: string;
  port: string;
  host: string;
}
