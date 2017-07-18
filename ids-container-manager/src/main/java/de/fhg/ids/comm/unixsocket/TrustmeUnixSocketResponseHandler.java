package de.fhg.ids.comm.unixsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustmeUnixSocketResponseHandler {
	private Logger LOG = LoggerFactory.getLogger(TrustmeUnixSocketResponseHandler.class);
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized byte[] waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(),e);
			}
		}
		return this.rsp;
	}
}

