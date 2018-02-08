import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { IncomingConnection } from './connections';
import { OutgoingConnection } from './connections';
import { Endpoint } from './connections';
import { ConnectionReportService } from './connectionReport.service';

@Component({
  selector: 'connections',
  templateUrl: './connectionReport.component.html',
  styleUrls: ['./connectionReport.component.css']
})

export class ConnectionReportComponent implements OnInit {

  incomingConnections: IncomingConnection[];
  outgoingConnections: OutgoingConnection[];
  endpoints: Endpoint[];

  constructor(private titleService: Title, private connectionService: ConnectionReportService) {
    this.titleService.setTitle('IDS Connections');
  }

  public ngOnInit(): void {
    this.connectionService.getIncomingConnections()
      .retryWhen(errors => errors.delay(5000).take(6))
      .subscribe(inConnections => {
        this.incomingConnections = inConnections;
      });

    this.connectionService.getOutgoingConnections()
      .retryWhen(errors => errors.delay(5000).take(6))
      .subscribe(outConnections => {
        this.outgoingConnections = outConnections;
      });

    this.connectionService.getEndpoints()
      .retryWhen(errors => errors.delay(5000).take(6))
      .subscribe(endpointList => {
        this.endpoints = endpointList;
      });
  }

}
