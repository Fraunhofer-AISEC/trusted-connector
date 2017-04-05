package de.fhg.ids.comm.unixsocket;

import jnr.unixsocket.UnixSocketChannel;

public class ChangeRequest {

	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	
	public UnixSocketChannel channel;
	public int type;
	public int ops;
	
	public ChangeRequest(UnixSocketChannel channel, int type, int ops) {
		this.channel = channel;
		this.type = type;
		this.ops = ops;
	}
}


