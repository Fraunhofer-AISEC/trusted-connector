import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/Rx";

import { Connection } from './connection';
import { ConnectionService } from './connectionReport.service';

@Component({
  selector: 'connections',
  templateUrl: './connectionsReport.component.html',
  styleUrls: ['./connectionsReport.component.css']
})

export class ConnectionReportComponent implements OnInit {

  connections: Connection[];

  constructor(private titleService: Title,  private connectionService: ConnectionService) {
     this.titleService.setTitle('Connections Statistics');
  }

  public ngOnInit(): void {
    this.connectionService.getIncommingConnections().subscribe(connections => {
       this.connections = connections;
       console.log(this.connections)
     });
  }

}
