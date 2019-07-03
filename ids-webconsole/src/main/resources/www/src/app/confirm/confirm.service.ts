import { Injectable } from '@angular/core';

@Injectable()
export class ConfirmService {
    public activate: (message?: string, title?: string) => Promise<boolean>;
}
