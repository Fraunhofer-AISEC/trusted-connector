import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Headers, Http, Response } from '@angular/http';

declare var API_URL: string;

@Component({
  selector: '[metricCard]',
  template: ` <div class="mdl-card mdl-shadow--2dp" style="min-height:10px!important;width:100%!important;min-width:20px!important">
               <div class="mdl-card__title mdl-card--expand" style="flex-direction:column;padding:12px">
                  <span style="font-size:30pt">{{value}}</span><br>
                  <span style="font-size:14pt">{{text}}</span>
               </div>
              </div>`,
})
export class MetricCardComponent implements OnInit {
  @Input('text') text:string = 'test';
  @Input('value') value:string = '0';
  @Input('value-url') valueUrl:string = null;
  @Output('valueChange') valueChange: EventEmitter<string> = new EventEmitter();

  private interval;

  constructor(private http: Http) { }

  ngOnInit() {
    // If remote date source is given, update value from there. Otherwise use static values
    if (this.valueUrl==null) {
      return;
    }

    // TODO use an Angular2 service to regularly poll metrics from the backend
    this.interval = setInterval(() => {
        this.getMetrics();
      }, 2000);
   }

  ngOnDestroy() {
    clearInterval(this.interval);
  }

  private getMetrics():void {
       this.http.get(this.valueUrl)
      .map(res => res.json())
      .subscribe(value => this.value = value);
  }
}
