import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/Rx";

import { IncommingConnection } from './connections';
import { OutgoingConnection } from './connections';
import { ConnectionService } from './inOutConnections.service';

@Component({
  selector: 'connections',
  templateUrl: './inOutConnections.component.html',
  styleUrls: ['./inOutConnections.component.css']
})

export class ConnectionReportComponent implements OnInit {

  incommingConnections: IncommingConnection[];
  outgoingConnections: OutgoingConnection[];

  constructor(private titleService: Title,  private connectionService: ConnectionService) {
     this.titleService.setTitle('IDS Connections');
  }

  public ngOnInit(): void {
    this.connectionService.getIncommingConnections().subscribe(inConnections => {
       this.incommingConnections = inConnections;
       console.log(this.incommingConnections)
     });

     this.connectionService.getOutgoingConnections().subscribe(outConnections => {
       this.outgoingConnections = outConnections;
       console.log(this.outgoingConnections)
     });
  }

}