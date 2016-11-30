# Installation
```
npm install
npm run postinstall
```

# Run (development)
```
gulp
```

You can then access the web server at http://localhost:5000.

To bundle the website for production mode:
```
NODE_ENV=production gulp bundle
```

The final bundle can be found in `dist`.

# Run (docker)
```
docker build -t registry.netsec.aisec.fraunhofer.de/iot-connector/iot-connector-dashboard .
docker run registry.netsec.aisec.fraunhofer.de/iot-connector/iot-connector-dashboard
```
