import { Component, OnInit, ViewContainerRef, EventEmitter, Output, OnDestroy } from '@angular/core';

import { Subscription } from 'rxjs/Subscription';

export class SubscriptionComponent implements OnDestroy {
  protected subscriptions: Subscription[] = [];

  ngOnDestroy(): void {
    console.log('Unsubscribing...');
    for (let subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }

}
