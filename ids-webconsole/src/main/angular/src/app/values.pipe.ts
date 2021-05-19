import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'values' })
export class ValuesPipe implements PipeTransform {
  public transform(value: any, _args?: any[]): any {
    return Object.keys(value)
      .map(key => value[key]);
  }
}
