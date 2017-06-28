package de.fhg.ids.cm.impl.trustx;

import jnr.unixsocket.UnixSocketChannel;

class ServerDataEvent {
	public TrustXMock server;
	public UnixSocketChannel socket;
	public byte[] data;
	
	public ServerDataEvent(TrustXMock server, UnixSocketChannel socket, byte[] data) {
		this.server = server;
		this.socket = socket;
		this.data = data;
	}
}