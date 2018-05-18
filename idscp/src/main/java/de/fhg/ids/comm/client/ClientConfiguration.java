package de.fhg.ids.comm.client;

import java.io.File;

import org.apache.camel.util.jsse.SSLContextParameters;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;

/**
 * Configuration of a client-side (Consumer) IDSC endpoint.
 * 
 * @author julian
 *
 */
public class ClientConfiguration {
	protected int port = 8080;
	protected File tpmdSocket;
	protected IdsAttestationType attestationType = IdsAttestationType.BASIC;
	protected SSLContextParameters params = null;
	protected int attestationMask = 0;

	public ClientConfiguration port(int port) {
		this.port = port;
		return this;
	}
	
	public ClientConfiguration tpmdSocket(File socket) {
		this.tpmdSocket = socket;
		return this;
	}
	
	public ClientConfiguration attestationMask(int attestationMask) {
		this.attestationMask = attestationMask;
		return this;
	}
	
	public ClientConfiguration attestationType(IdsAttestationType attestationType) {
		this.attestationType = attestationType;
		return this;
	}
	

}
