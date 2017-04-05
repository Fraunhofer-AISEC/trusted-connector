/*
	Example of a very simple MQTT consumer app.

	This app subscribes to events at a public MQTT broker and makes them 
	available by a REST API. For demonstration purposes, it continuously 
	publishes random values under the subscribed MQTT topic.

	(C) Fraunhofer AISEC, 2017
*/
var temp = 0

// Start REST server
var express = require('express');
var app = express();

// Start MQTT client
const mqtt = require('mqtt')
const client = mqtt.connect('mqtt://iot.eclipse.org')

// React on MQTT connection
client.on('connect', () => {
    console.log("Connected to MQTT broker")
    client.subscribe('ids-example-001/temp')
})

// React on received MQTT messages
client.on('message', (topic, message) => {
  switch (topic) {
    case 'ids-example-001/temp':
      return handleTemperature(message)
  }
  console.log('No handler for topic %s', topic)
})

// Handle a temp value received over MQTT
function handleTemperature (message) {
  console.log('Received temp %s', message)
  temp = message
}

// Publish MQTT event
function publishEvent () {  
    value = Math.random() * (35 - 10) + 10
    console.log('Publishing temp %s', value)
    client.publish('ids-example-001/temp',  String(value))
}

// simulate sensor data by publishing random data to MQTT broker
setInterval(() => {
  publishEvent()
}, 1000)

// Start REST endpoint /temp
app.get('/temp', function (req, res) {
   res.end(temp)
})

var server = app.listen(8080, function () {
  var host = server.address().address
  var port = server.address().port
  console.log("REST API listening at http://%s:%s", host, port)
})