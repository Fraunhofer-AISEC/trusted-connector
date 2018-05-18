package de.fhg.ids.comm.server;

import java.io.File;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;

/**
 * Configuration of the server-side (Provider) part of the IDSC protocol.
 * 
 * @author julian
 *
 */
public class ServerConfiguration {
	protected int port = 8080;
	protected String basePath = "/";
	protected IdsAttestationType attestationType;
	protected File tpmdSocket;
	protected int attestationMask;
	
	public ServerConfiguration port(int port) {
		this.port = port;
		return this;
	}
	
	public ServerConfiguration basePath(String basePath) {
		this.basePath = basePath;
		return this;
	}
	
	public ServerConfiguration tpmdSocket(File socket) {
		this.tpmdSocket = socket;
		return this;
	}
	
	public ServerConfiguration attestationMask(int mask) {
		this.attestationMask = mask;
		return this;
	}
	
	public ServerConfiguration attestationType(IdsAttestationType type) {
		this.attestationType = type;
		return this;
	}

}
