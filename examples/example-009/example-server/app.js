/*
  Example of a very simple REST consumer app.

  This app accepts sensor values from POST requests to 
  http://<host>:8081/temp
  and displays them under
  http://<host>:8081/
  
  For demonstration purposes only.

  (C) Fraunhofer AISEC, 2017
*/
var temp = 0

// Start REST server
var express = require('express')
  , app = express()

// just use raw body data
var bodyParser = require('body-parser')
var options = {
  inflate: true,
  limit: '10kb',
  type: 'text/xml'
};
app.use( bodyParser.raw(options) );

// Start REST endpoint /temp
app.post('/temp', function (req, res) {
  temp = req.body
  console.log('received temp ' + temp)
  res.end('OK')
})

// Start web page /
app.get('/', function (req, res, next) {
  try {
    var html = '<html><body><h1>Temp '+Number(temp).toFixed(2)+'</h1><script>function refresh () {window.location.reload(true);}; window.setTimeout(refresh, 1000);</script></body></html>'
    res.send(html)
  } catch (e) {
    next(e)
  }
})

var server = app.listen(8081, function () {
  var host = server.address().address
  var port = server.address().port
  console.log("REST API listening at http://%s:%s", host, port)
})
