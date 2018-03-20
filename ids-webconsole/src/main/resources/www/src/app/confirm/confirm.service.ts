import { Injectable } from '@angular/core';

@Injectable()
export class ConfirmService {
    activate: (message?: string, title?: string) => Promise<boolean>;
}
