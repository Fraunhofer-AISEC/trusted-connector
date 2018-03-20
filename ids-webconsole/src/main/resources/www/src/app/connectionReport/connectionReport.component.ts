import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Endpoint, IncomingConnection, OutgoingConnection } from './connections';
import { ConnectionReportService } from './connectionReport.service';

@Component({
  selector: 'connections',
  templateUrl: './connectionReport.component.html',
  styleUrls: ['./connectionReport.component.css']
})
export class ConnectionReportComponent implements OnInit {

  incomingConnections: Array<IncomingConnection>;
  outgoingConnections: Array<OutgoingConnection>;
  endpoints: Array<Endpoint>;

  constructor(private titleService: Title, private connectionService: ConnectionReportService) {
    this.titleService.setTitle('IDS Connections');
  }

  ngOnInit(): void {
    this.connectionService.getIncomingConnections()
      .retryWhen(errors => errors.delay(5000)
        .take(6))
      .subscribe(inConnections => {
        this.incomingConnections = inConnections;
      });

    this.connectionService.getOutgoingConnections()
      .retryWhen(errors => errors.delay(5000)
        .take(6))
      .subscribe(outConnections => {
        this.outgoingConnections = outConnections;
      });

    this.connectionService.getEndpoints()
      .retryWhen(errors => errors.delay(5000)
        .take(6))
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
