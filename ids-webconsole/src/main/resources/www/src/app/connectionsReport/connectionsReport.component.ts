import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/Rx";


@Component({
  selector: 'connections',
  templateUrl: './connectionsReport.component.html',
  styleUrls: ['./connectionsReport.component.css']
})

export class ConnectionReportComponent implements OnInit {

  inCounter: number = 0;
  outCounter: number = 0;

  private inConnections: BehaviorSubject<Array<Connection>> = new BehaviorSubject([]);
  public _inConnections: Observable<Array<Connection>> = this.inConnections.asObservable();

  private outConnections: BehaviorSubject<Array<Connection>> = new BehaviorSubject([]);
  public _outConnections: Observable<Array<Connection>> = this.outConnections.asObservable();

  constructor(private titleService: Title, private element: ElementRef) {
     this.titleService.setTitle('Connections Statistics');
  }

  public ngOnInit(): void {
  }

  inConns = Observable.interval(1500)
              .subscribe(() => {
                let tim = new Date(new Date().getTime() + 10000);
                this.inCounter = this.inCounter + 1;
                let con = new Connection( this.inCounter, "10.244.32.13", "aisec.fraunhofer.de", tim.toString());
                this.inConnections.next(this.inConnections.getValue().concat(con));
          });


  outConns = Observable.interval(2000)
              .subscribe(() => {
                let tim = new Date(new Date().getTime() + 10000);
                this.outCounter = this.outCounter + 1;
                let con = new Connection( this.outCounter, "10.244.32.10", "aisec.fraunhofer.de", tim.toString());
                this.outConnections.next(this.outConnections.getValue().concat(con));
          });
}

class Connection {
  id: number;
  ip: string;
  name: string;
  time: string;

  constructor(ID: number,IP: string, inName: string, inTime: string) {
        this.id = ID;
        this.ip = IP;
        this.name = inName;
        this.time = inTime;
    }
}
