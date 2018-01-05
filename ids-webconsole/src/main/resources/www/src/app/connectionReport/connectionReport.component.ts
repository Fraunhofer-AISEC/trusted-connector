import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/BehaviorSubject";

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
  
  constructor(private titleService: Title,  private connectionService: ConnectionReportService) {
     this.titleService.setTitle('IDS Connections');
  }

  public ngOnInit(): void {
    this.connectionService.getIncomingConnections().subscribe(inConnections => {
       this.incomingConnections = inConnections;
       console.log("incomingConnections");
       console.log(this.incomingConnections)
     });

     this.connectionService.getOutgoingConnections().subscribe(outConnections => {
       this.outgoingConnections = outConnections;
       console.log("outgoingConnections" + typeof this.outgoingConnections);
       console.log(this.outgoingConnections)
     });
     
     this.connectionService.getEndpoints().subscribe(endpointList => {
       this.endpoints = endpointList;
       console.log("endpoints" + typeof this.endpoints);
       console.log(this.endpoints)
     });
  }

}
