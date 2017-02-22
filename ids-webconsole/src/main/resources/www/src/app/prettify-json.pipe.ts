import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'prettify'
})
export class PrettifyPipe implements PipeTransform {
  transform(val) {

    var joj =  JSON.stringify(val)
        .replace('\n', '<br/><span>ooookkkkkk</span>')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
              var cls = 'number';
              if (/^"/.test(match)) {
                  if (/:$/.test(match)) {
                      cls = 'key';
                  } else {
                      cls = 'string';
                  }
              } else if (/true|false/.test(match)) {
                  cls = 'boolean';
              } else if (/null/.test(match)) {
                  cls = 'null';
              }
              return   match ;
          });
    var jsonObject = JSON.parse(joj)
    return jsonObject;
  }
}
