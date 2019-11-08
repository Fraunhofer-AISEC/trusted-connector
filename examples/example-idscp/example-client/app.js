/*
	Example of a very simple MQTT consumer app.

	This app subscribes to events at a public MQTT broker and makes them 
	available by a REST API. For demonstration purposes, it continuously 
	publishes random values under the subscribed MQTT topic.

	(C) Fraunhofer AISEC, 2017
*/

// Start MQTT client
const mqtt = require('mqtt');
const client = mqtt.connect('mqtt://mqtt-broker');
const TEMP_TOPIC = 'ids-example-010/temp';

// React on MQTT connection
client.on('connect', () => {
    console.log("Connected to MQTT broker")
    client.subscribe(TEMP_TOPIC)
})

// React on received MQTT messages
client.on('message', (topic, message) => {
  switch (topic) {
    case TEMP_TOPIC:
      console.log('Published and Received temp %s', message);
      break;
    default:
      console.log('No handler for topic %s', topic);
  }
})

// simulate sensor data by publishing random temperature data to MQTT broker
setInterval(() => {
    client.publish(TEMP_TOPIC,  String(Math.random() * (100 - 10) + 100));
}, 1000)
