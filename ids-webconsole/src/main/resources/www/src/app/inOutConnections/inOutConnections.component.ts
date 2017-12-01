import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/Rx";

import { IncomingConnection } from './connections';
import { OutgoingConnection } from './connections';
import { ConnectionInOutService } from './inOutConnections.service';

@Component({
  selector: 'connections',
  templateUrl: './inOutConnections.component.html',
  styleUrls: ['./inOutConnections.component.css']
})

export class ConnectionInOutComponent implements OnInit {

  incomingConnections: IncomingConnection[];
  outgoingConnections: OutgoingConnection[];

  constructor(private titleService: Title,  private connectionService: ConnectionInOutService) {
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
  }

}
