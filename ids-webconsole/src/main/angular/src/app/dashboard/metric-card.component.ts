import { Component, Input } from '@angular/core';

@Component({
  selector: '[metricCard]',
  templateUrl: './metric-card.component.html'
})
export class MetricCardComponent {
  @Input() public text = 'test';
  @Input() public value = '0';
}
