export class IncomingConnection {
  public endpointIdentifier: string;
  public attestationResult: string;
  public connectionKey: string;
  public remoteHostName: string;
}

export class OutgoingConnection {
  public endpointIdentifier: string;
  public remoteAuthentication: string;
  public remoteIdentity: string;
  public attestationResult: string;
}

export class Endpoint {
  public endpointIdentifier: string;
  public defaultProtocol: string;
  public port: string;
  public host: string;
}
