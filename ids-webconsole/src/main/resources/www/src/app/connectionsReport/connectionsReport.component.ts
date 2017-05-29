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

  i: number = 0;

  private connections: BehaviorSubject<Array<Connection>> = new BehaviorSubject([]);
  public _connections: Observable<Array<Connection>> = this.connections.asObservable();

  constructor(private titleService: Title, private element: ElementRef) {
     this.titleService.setTitle('Connections Statistics');
  }

 public ngOnInit(): void {
  }

  conns = Observable.interval(1500)
              .subscribe(() => {
               let tim = new Date(new Date().getTime() + 10000);
                this.i = this.i + 1;
                let con = new Connection( this.i, "10.244.32.13", "aisec.fraunhofer.de", tim.toString());
                this.connections.next(this.connections.getValue().concat(con));
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
