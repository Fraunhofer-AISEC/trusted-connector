#!/usr/bin/env python
from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
from urlparse import urlparse, parse_qs
import cgi
import json
import base64
 
with open('mock/sigpubkey.bin', 'rb') as f:
    data = f.read()
     
class RestHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        params = parse_qs(urlparse(self.path).query)
	if self.path == "/cert.pub":
		self.send_header('Content-type','application/json')	
		self.end_headers()	
		self.wfile.write(base64.b64encode(data))
	else:
		self.end_headers()
		self.wfile.write("error: nothing to see here")
	return
     
    def do_POST(self):
        form = cgi.FieldStorage(fp=self.rfile,
                           headers=self.headers, environ={
                                'REQUEST_METHOD':'POST', 
                                'CONTENT_TYPE':self.headers['Content-Type']
                           })
        pcr = form['pcr'].value
        self.send_response(201)
        self.end_headers()
        self.wfile.write(pcr)
        return
     
httpd = HTTPServer(('0.0.0.0', 7331), RestHTTPRequestHandler)
httpd.serve_forever()


