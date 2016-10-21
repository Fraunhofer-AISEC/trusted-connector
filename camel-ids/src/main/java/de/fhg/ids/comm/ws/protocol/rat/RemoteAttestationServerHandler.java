package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class RemoteAttestationServerHandler {
	private final FSM fsm;
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private boolean attestationSucccessfull = false;
	private Thread thread;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationServerHandler.class);
	private UnixSocketThread client;
	private UnixSocketResponsHandler handler;
	
	public RemoteAttestationServerHandler(FSM fsm, IdsAttestationType type) {
		this.fsm = fsm;
		this.aType = type;
		try {
			this.client = new UnixSocketThread();
			this.thread = new Thread(client);
			this.thread.setDaemon(true);
			this.thread.start();
			this.handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			LOG.debug("could not initialze thread!");
			e.printStackTrace();
		}		
	}

	public MessageLite sendTPM2Ddata(Event e) {
		this.myNonce = NonceGenerator.generate();
		this.yourNonce = e.getMessage().getAttestationRequest().getQualifyingData().toString();
		
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(this.aType)
				.setQualifyingData(this.yourNonce)
				.setCode(Code.INTERNAL_ATTESTATION_REQ)
				.build();
		
		try {
			client.send(msg.toByteArray(), this.handler);
			TpmToController answer = this.handler.waitForResponse();
			
			LOG.debug("got msg from tpm2d:" + answer.toString());
			
			// TODO: check answer with tpp here
			
			return ConnectorMessage
					.newBuilder()
					.setId(0)
					.setType(ConnectorMessage.Type.RAT_RESPONSE)
					.setAttestationResponse(
							AttestationResponse
							.newBuilder()
							.setAtype(this.aType)
							.setQualifyingData(this.myNonce)
							.setHalg("")
							.setQuoted("")
							.setSignature("")
							//.setPcrValue(0, 
							//		Proto3Pcr
							//		.newBuilder()
							//		.setNumber(0)
							//		.setValue("")
							//		.build())
							.setCertificateUri("")
							.build()
							)
					.build();			
		} catch (IOException e1) {
			LOG.debug("IOException when writing to unix socket");
			e1.printStackTrace();
			return ConnectorMessage
					.newBuilder()
					.build();
		}
		
		
		// todo:
		// local TPM2d communication here
		// in order to get atype, halg etc
		

	}

	public MessageLite sendResult(Event e) {
		// todo:
		// TPP check of PCR values & sign & quote etc
		// and set attestationSucccessfull 
		this.attestationSucccessfull  = false;
				
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_RESULT)
				.setAttestationResult(
						AttestationResult
						.newBuilder()
						.setAtype(this.aType)
						.setResult(this.attestationSucccessfull)
						.build()
						)
				.build();
	}

	public MessageLite leaveRatRequest(Event e) {
		this.thread.interrupt();
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_LEAVE)
				.setAttestationLeave(
						AttestationLeave
						.newBuilder()
						.setAtype(this.aType)
						.build()
						)
				.build();
	}
}
