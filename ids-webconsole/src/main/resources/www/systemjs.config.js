/**
 * System configuration for Angular samples
 * Adjust as necessary for your application needs.
 */
 (function (global) {
   System.config({
     packageConfigPaths: ['./node_modules/*/package.json', './node_modules/@*/*/package.json'],
     paths: {
       'app': 'app',
       '*': 'node_modules/*'
     },
     packages: {
       app: {
         main: './main.js',
         defaultExtension: 'js'
       },
       rxjs: {
         defaultExtension: 'js'
       },
       d3: {
         main: './build/d3.min.js',
       }
     }
   });
})(this);
