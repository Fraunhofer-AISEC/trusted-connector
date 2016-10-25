#!/usr/bin/env python
from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
from urlparse import urlparse, parse_qs
import cgi
import json
     
TODOS = [
    {'id': 1, 'title': 'learn python'},
    {'id': 2, 'title': 'get paid'},
]

with open('sigpubkey.bin', 'rb') as f:
    data = f.read()
     
class RestHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        params = parse_qs(urlparse(self.path).query)
	if self.path == "/cert.pub":
		self.wfile.write(data)	
	else:
		self.wfile.write("error: nothing to see here")
	return
     
    def do_POST(self):
        new_id = max(filter(lambda x: x['id'], TODOS))['id'] + 1
        form = cgi.FieldStorage(fp=self.rfile,
                           headers=self.headers, environ={
                                'REQUEST_METHOD':'POST', 
                                'CONTENT_TYPE':self.headers['Content-Type']
                           })
        new_title = form['title'].value
        new_todo = {'id': new_id, 'title': new_title}
        TODOS.append(new_todo)
   
        self.send_response(201)
        self.end_headers()
        self.wfile.write(json.dumps(new_todo))
        return
     
httpd = HTTPServer(('0.0.0.0', 7331), RestHTTPRequestHandler)
while True:
    httpd.handle_request()
