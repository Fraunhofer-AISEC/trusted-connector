import { Component, Input } from '@angular/core';

@Component({
  selector: '[metricCard]',
  templateUrl: './metric-card.component.html'
})
export class MetricCardComponent {
  @Input() text = 'test';
  @Input() value = '0';
}
