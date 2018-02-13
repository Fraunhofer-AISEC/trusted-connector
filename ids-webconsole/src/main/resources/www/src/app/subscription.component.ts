import { OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';

export class SubscriptionComponent implements OnDestroy {
  protected subscriptions: Array<Subscription> = [];

  ngOnDestroy(): void {
    // console.log('Unsubscribing...');
    for (const subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }

}
