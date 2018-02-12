import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'prettify'
})
export class PrettifyPipe implements PipeTransform {
  transform(val): Object {
    const obj =  JSON.stringify(val)
        .replace('\n', '<br />')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    return JSON.parse(obj);
  }
}
