import { OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';

export class SubscriptionComponent implements OnDestroy {
  protected subscriptions: Array<Subscription> = [];

  public ngOnDestroy(): void {
    // console.log('Unsubscribing...');
    for (const subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }

}
