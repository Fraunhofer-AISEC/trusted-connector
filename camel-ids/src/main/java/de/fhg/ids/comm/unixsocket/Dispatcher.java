package de.fhg.ids.comm.unixsocket;

public interface Dispatcher {
	public String SOCKET = "/tpm/socket";
	public boolean receive();
	public boolean dispatch();
}
