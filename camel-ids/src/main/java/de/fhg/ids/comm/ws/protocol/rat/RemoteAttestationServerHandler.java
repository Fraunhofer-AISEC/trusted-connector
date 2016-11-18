package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.security.PublicKey;
import java.security.Signature;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.Pcr;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;

public class RemoteAttestationServerHandler extends RemoteAttestationHandler {
	private final FSM fsm;
	private String SOCKET = "mock/socket/tpm2ds.sock";
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Thread thread;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationServerHandler.class);
	private UnixSocketThread client;
	private UnixSocketResponsHandler handler;
	private byte[] yourQuoted;
	private byte[] yourSignature;
	private String certUri;
	private boolean signatureCorrect;
	private Pcr[] pcrValues;
	
	public RemoteAttestationServerHandler(FSM fsm, IdsAttestationType type) {
		// set finite state machine
		this.fsm = fsm;
		// set current attestation type (see attestation.proto)
		this.aType = type;
		// try to start new Thread:
		// UnixSocketThread will be used to communicate with local TPM2d		
		try {
			// client will be used to send messages
			this.client = new UnixSocketThread(this.SOCKET);
			this.thread = new Thread(client);
			this.thread.setDaemon(true);
			this.thread.start();
			// responseHandler will be used to wait for messages
			this.handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			LOG.debug("could not write to/read from " + SOCKET);
			e.printStackTrace();
		}		
	}

	public MessageLite sendTPM2Ddata(Event e) {
		// generate a new software nonce on the server
		this.myNonce = NonceGenerator.generate();
		// get the current client nonce from the message
		this.yourNonce = e.getMessage().getAttestationRequest().getQualifyingData().toString();
		// set the current attestation to the same as the client
		this.aType = e.getMessage().getAttestationRequest().getAtype();
		// construct protobuf message to send to local tpm2d via unix socket
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(this.aType)
				.setQualifyingData(this.yourNonce)
				.setCode(Code.INTERNAL_ATTESTATION_REQ)
				.build();		
		// try to talk to local unix socket
		try {
			if(thread.isAlive()) {
				// send msg to local unix socket
				client.send(msg.toByteArray(), this.handler);
				TpmToController answer = this.handler.waitForResponse();
				// now return values from answer to client
				return ConnectorMessage
						.newBuilder()
						.setId(0)
						.setType(ConnectorMessage.Type.RAT_RESPONSE)
						.setAttestationResponse(
								AttestationResponse
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.myNonce)
								.setHalg(answer.getHalg())
								.setQuoted(answer.getQuoted())
								.setSignature(answer.getSignature())
								.addAllPcrValues(answer.getPcrValuesList())
								.setCertificateUri(answer.getCertificateUri())
								.build()
								)
						.build();				
			}
			else {
				LOG.debug("error: thread is not alive");
				return null;
			}
		} catch (IOException e1) {
			LOG.debug("IOException when writing to unix socket");
			e1.printStackTrace();
			return null;
		} catch (InterruptedException e1) {
			LOG.debug("InterruptedException when writing to unix socket");
			e1.printStackTrace();
			return null;
		}
	}

	public MessageLite sendResult(Event e) {
		Signature sg;
		// get nonce from server msg
		this.yourNonce = e.getMessage().getAttestationResponse().getQualifyingData().toString();
		// get quote from server msg
		this.yourQuoted = DatatypeConverter.parseHexBinary(e.getMessage().getAttestationResponse().getQuoted());
		// get signature from server msg
		this.yourSignature = DatatypeConverter.parseHexBinary(e.getMessage().getAttestationResponse().getSignature());
		// get cert uri from server msg
		this.certUri = e.getMessage().getAttestationResponse().getCertificateUri();
		// get pcr values from server msg
		int numPcrValues = e.getMessage().getAttestationResponse().getPcrValuesCount();
		this.pcrValues = e.getMessage().getAttestationResponse().getPcrValuesList().toArray(new Pcr[numPcrValues]);
		try {
			// construct a new TPM2B_PUBLIC from bkey bytes
			TPM2B_PUBLIC key = new TPM2B_PUBLIC(this.fetchPublicKey(this.certUri));
			// and convert it into an DER key
			PublicKey publicKey = new PublicKeyConverter(key).getPublicKey();
			// construct a new TPMT_SIGNATURE from yourSignature bytes
			TPMT_SIGNATURE tpmSignature = new TPMT_SIGNATURE(this.yourSignature);
			// construct a new TPMS_ATTEST from yourQuoted bytes
			TPMS_ATTEST digest = new TPMS_ATTEST(this.yourQuoted);
			// and get the raw byte quote
			switch(tpmSignature.getSignature().getHashAlg()) {
				case TPM_ALG_SHA256:
					sg = Signature.getInstance("SHA256withRSA");
				    sg.initVerify(publicKey);
				    sg.update(digest.toBytes());
				    this.signatureCorrect = sg.verify(tpmSignature.getSignature().getSig());
					break;
				case TPM_ALG_SHA1:
					sg = Signature.getInstance("SHA1withRSA");
				    sg.initVerify(publicKey);
				    sg.update(digest.toBytes());
				    this.signatureCorrect = sg.verify(tpmSignature.getSignature().getSig());
					break;					
				default:
					break;
			}
			
		} catch (Exception ex) {
			LOG.debug("error: exception " + ex.getMessage());
			ex.printStackTrace();
		}
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_RESULT)
				.setAttestationResult(
						AttestationResult
						.newBuilder()
						.setAtype(this.aType)
						.setResult(this.attestationSuccessful(this.signatureCorrect, this.pcrValues))
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
