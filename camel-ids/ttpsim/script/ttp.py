#!/usr/bin/env python3
from http.server import HTTPServer
from http.server import BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import simplejson
import cgi
import json
import base64
     
class RestHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        params = parse_qs(urlparse(self.path).query)
        self.send_header('Content-type', 'text/html') 
        self.end_headers()
        self.wfile.write("<html><head><title>404</title></head><body>nothing to see here .. this is just a simple ttp mock</body></html>".encode('utf-8'))
        return

    def do_POST(self):
        self.data_string = self.rfile.read(int(self.headers['Content-Length']))
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
	self.send_header("Content-Length", self.headers['Content-Length'])
        self.end_headers()
        data = simplejson.loads(self.data_string)
        data["success"] = True
        self.wfile.write(simplejson.dumps(data).encode('utf-8'))
        return
  
httpd = HTTPServer(('0.0.0.0', 29663), RestHTTPRequestHandler)
httpd.serve_forever()
