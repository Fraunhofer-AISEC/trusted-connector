package de.fhg.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.Proto3Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.ids.comm.unixsocket.UNIXReadWriteDispatcher;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class RemoteAttestationServerHandler {
	private final FSM fsm;
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private boolean attestationSucccessfull = false;
	
	public RemoteAttestationServerHandler(FSM fsm, IdsAttestationType type) {
		this.fsm = fsm;
		this.aType = type;

	}

	public MessageLite sendTPM2Ddata(Event e) {
		this.myNonce = NonceGenerator.generate();
		this.yourNonce = e.getMessage().getAttestationRequest().getQualifyingData();
		System.out.println("myNonce:" + this.myNonce + "\nyourNonce:" + this.yourNonce);
		// todo:
		// local TPM2d communication here
		// in order to get atype, halg etc
		
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
