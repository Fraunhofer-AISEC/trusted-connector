package de.fhg.ids.comm.unixsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnixSocketResponsHandler {
	private Logger LOG = LoggerFactory.getLogger(UnixSocketResponsHandler.class);
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized void waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
		LOG.debug("a unix socket msg arrived:" + new String(this.rsp));
	}
}

