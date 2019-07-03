import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { delay, retryWhen, take } from 'rxjs/operators';

import { ConnectionReportService } from './connection-report.service';
import { Endpoint, IncomingConnection, OutgoingConnection } from './connections';

@Component({
  selector: 'connections',
  templateUrl: './connection-report.component.html',
  styleUrls: ['./connection-report.component.css']
})
export class ConnectionReportComponent implements OnInit {

  public incomingConnections: Array<IncomingConnection>;
  public outgoingConnections: Array<OutgoingConnection>;
  public endpoints: Array<Endpoint>;

  constructor(private readonly titleService: Title, private readonly connectionService: ConnectionReportService) {
    this.titleService.setTitle('IDS Connections');
  }

  public ngOnInit(): void {
    const errorStrategy = errors => errors.pipe(
      delay(5000),
      take(6)
    );

    this.connectionService.getIncomingConnections()
      .pipe(retryWhen(errorStrategy))
      .subscribe(inConnections => {
        this.incomingConnections = inConnections;
      });

    this.connectionService.getOutgoingConnections()
      .pipe(retryWhen(errorStrategy))
      .subscribe(outConnections => {
        this.outgoingConnections = outConnections;
      });

    this.connectionService.getEndpoints()
      .pipe(retryWhen(errorStrategy))
      .subscribe(endpointList => {
        this.endpoints = endpointList;
      });
  }

  public trackEndpoints(index: number, item: Endpoint): string {
    return item.endpointIdentifier;
  }

  public trackIncoming(index: number, item: IncomingConnection): string {
    return item.connectionKey;
  }

  public trackOutgoing(index: number, item: OutgoingConnection): string {
    return item.endpointIdentifier + item.remoteIdentity;
  }

}
