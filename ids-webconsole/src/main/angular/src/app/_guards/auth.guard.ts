import { Injectable } from '@angular/core';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

/**
 * Ensures that only logged-in users will be navigated to pages.
 * The actual access control has to take place on the backend side, obviously.
 * See app.routing.ts for usage of this guard.
 */
@Injectable()
export class AuthGuard  {

    constructor(private router: Router) {
    }

    // noinspection JSUnusedGlobalSymbols
    public canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        if (sessionStorage.getItem('currentUser')) {
            // logged in so return true
            return true;
        }

        // not logged in so redirect to login page
        this.router.navigate(['/login'], {queryParams: {returnUrl: state.url}});
        return false;
    }
}
