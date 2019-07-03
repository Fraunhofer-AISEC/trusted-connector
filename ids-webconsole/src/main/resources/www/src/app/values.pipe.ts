import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'values' })
export class ValuesPipe implements PipeTransform {
  public transform(value: any, args?: Array<any>): any {
    return Object.keys(value)
      .map(key => value[key]);
  }
}
