import { Component, OnInit, ViewContainerRef, EventEmitter, Output } from '@angular/core';

import { Subscription } from 'rxjs/Subscription';

export class SubscriptionComponent {
  protected subscriptions: Subscription[] = [];

  ngOnDestroy(): void {
    console.log('Unsubscribing...');
    for(let subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }

}
