#!/usr/bin/env python
import socket
import sys
import os
import attestation_pb2

address = '/tmp/tpm2d.sock'

# Make sure the socket does not already exist
try:
    os.unlink(address)
except OSError:
    if os.path.exists(address):
        raise

# Create a UDS socket
sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
# Bind the socket to the port
print >>sys.stderr, 'starting up on %s' % address
sock.bind(address)

# Listen for incoming connections
sock.listen(1)

att = attestation_pb2.TpmToController()
att.halg = "TPM_ALG_SHA256"
att.quoted = "ff54434780180022000b387cfdf4579101b479c6ed2ad4c473e7a565cdb05cc62d6221dbd76f02ab2bdd000361616100000000000000a5000000000000000001201605110016280000000001000b030300000020f5a5fd42d16a20302798ef6ed309979b43003d2320d9f0e8ea9831a9"
att.signature = "0014000b0100447a3def49270c1ca0af27b035020295a8362bb35e86e214a2b77ebbe534be1162c714475f294a87c4e64c19d8738c59419f20cde32c38e81c7522e9c923641aabec62a80200af654b2a6675fcd01957e6fbbfe139d067feb60e727960089622b2a733a1de64e289c931fe7e015894fa84cd24eb85a80d83eb190056f140929454ab9edcdf2471cbe61767b70dc778f56cc0ee13088b8f6a43dd8bb687fd05310ac7d0d91d919dce9a6c72e9a8504c19548157f3a1802c15fd91abb4007386c04bfa608f334473a0caaf353e83efddbaceae829f292be4226edb5bf2c3be587ee783667223b2ebd22a7a4985b788a143f7f37106898db72d8cd8a1999071fcc2"
att.certificate_uri = "https://irgendeinnonsense/"

send = att.SerializeToString()

while True:
    # Wait for a connection
    print >>sys.stderr, 'waiting for a connection'
    connection, address = sock.accept() 
    try:
        print >>sys.stderr, 'connection from ', address
        while True:
            data = connection.recv(1024)
            if data:
		msg = attestation_pb2.ControllerToTpm()
		msg.ParseFromString(data)
		print >>sys.stderr, 'got nonce: %s' % msg.qualifyingData	
		print >>sys.stderr, 'sending out:\n%s' % send
		connection.sendall(send)
            else:
                print >>sys.stderr, 'no more data from %s' % address
                break

    finally:
        # Clean up the connection
        connection.close()	
        # delete open socket
	try:
            os.unlink(address)
	except OSError:
    	    pass

