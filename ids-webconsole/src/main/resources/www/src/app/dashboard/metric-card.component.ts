import { Component, EventEmitter, Input, Output } from '@angular/core';

declare var API_URL: string;

@Component({
  selector: '[metricCard]',
  templateUrl: './metric-card.component.html'
})
export class MetricCardComponent {
  @Input('text') text = 'test';
  @Input('value') value = '0';
  @Output('valueChange') valueChange: EventEmitter<string> = new EventEmitter();

  private interval;
}
