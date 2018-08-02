import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Endpoint, IncomingConnection, OutgoingConnection } from './connections';
import { ConnectionReportService } from './connection-report.service';
import { delay, retryWhen, take } from 'rxjs/operators';

@Component({
  selector: 'connections',
  templateUrl: './connection-report.component.html',
  styleUrls: ['./connection-report.component.css']
})
export class ConnectionReportComponent implements OnInit {

  incomingConnections: Array<IncomingConnection>;
  outgoingConnections: Array<OutgoingConnection>;
  endpoints: Array<Endpoint>;

  constructor(private titleService: Title, private connectionService: ConnectionReportService) {
    this.titleService.setTitle('IDS Connections');
  }

  ngOnInit(): void {
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

  trackEndpoints(index: number, item: Endpoint): string {
    return item.endpointIdentifier;
  }

  trackIncoming(index: number, item: IncomingConnection): string {
    return item.connectionKey;
  }

  trackOutgoing(index: number, item: OutgoingConnection): string {
    return item.endpointIdentifier + item.remoteIdentity;
  }

}
