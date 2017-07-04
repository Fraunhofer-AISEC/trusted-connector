import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/Rx";

import { Connection } from './connection';
import { ConnectionService } from './inOutConnections.service';

@Component({
  selector: 'connections',
  templateUrl: './inOutConnections.component.html',
  styleUrls: ['./inOutConnections.component.css']
})

export class ConnectionReportComponent implements OnInit {

  incommingConnections: Connection[];
  outgoingConnections: Connection[];

  constructor(private titleService: Title,  private connectionService: ConnectionService) {
     this.titleService.setTitle('Connections Statistics');
  }

  public ngOnInit(): void {
    this.connectionService.getIncommingConnections().subscribe(connections => {
       this.incommingConnections = connections;
       console.log(this.incommingConnections)
     });

     this.connectionService.getOutgoingConnections().subscribe(connections => {
       this.outgoingConnections = connections;
       console.log(this.outgoingConnections)
     });
  }

}