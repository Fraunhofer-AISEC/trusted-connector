import { Component, OnInit, ElementRef } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { Network, DataSet, Node, Edge, IdType, Timeline } from 'vis';

import { Observable } from 'rxjs/Observable';

import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/observable/interval';
import {BehaviorSubject} from "rxjs/Rx";

@Component({
  selector: 'data-flow',
  templateUrl: './dataFlow.component.html',
  styleUrls: ['./dataFlow.component.css']
})

export class DataFlowComponent implements OnInit {
  i: number = 0;
  items: any;

  private numbers: BehaviorSubject<Array<Package>> = new BehaviorSubject([]);
  public _numbers: Observable<Array<Package>> = this.numbers.asObservable();

  constructor(private titleService: Title, private element: ElementRef) {
     this.titleService.setTitle('Data Flow Presentation');
  }
  public ngOnInit(): void {
    var container = document.getElementById('visualization');
    // Create a DataSet (allows two way data-binding)
    this.items = new DataSet([
    ]);

    // Configuration for the Timeline
    var options = {
    start: new Date(),
     end: new Date(new Date().getTime() + 20000),
      rollingMode: true
    };
    container.style.visibility = 'visible';
    // Create a Timeline
    var timeline = new Timeline(container, this.items, options);
  }

  num = Observable.interval(1500)
              .subscribe(() => {
                let tim = new Date(new Date().getTime() + 10000);
                this.i = this.i + 1;
                this.items.add({
                     id: this.i,
                     content: "packet " + this.i,
                     start: tim
                 });
                 let pac = new Package( this.i, tim.toString() ,Math.ceil( Math.random()* 400));
                 this.numbers.next(this.numbers.getValue().concat(pac));
              });
}

class Package {
  id: number;
  value: number;
  time: string;
  length: number;
  constructor(ID: number, inTime: string, inLength: number) {
        this.id = ID;
        this.value = 1;
        this.time = inTime;
        this.length = inLength;
    }
}
