package de.fhg.ids.comm.unixsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;

public class UnixSocketResponsHandler {
	private Logger LOG = LoggerFactory.getLogger(UnixSocketResponsHandler.class);
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		this.notify();
		return true;
	}
	
	public synchronized TpmToController waitForResponse() {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
		try {
			return TpmToController.parseFrom(this.rsp);
		} catch (InvalidProtocolBufferException e) {
			LOG.debug("could not parse protobuf msg TpmToController from tpm2d");
			e.printStackTrace();
			return TpmToController.newBuilder().build();
		}
	}
}

