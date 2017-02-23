import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'prettify'
})
export class PrettifyPipe implements PipeTransform {
  transform(val) {
    var obj =  JSON.stringify(val)
        .replace('\n', '<br/>')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    var jsonObject = JSON.parse(obj)
    return jsonObject;
  }
}
