package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.attestation.PcrValue;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;

public class RemoteAttestationProviderHandler extends RemoteAttestationHandler {
	private final FSM fsm;
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Thread thread;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationProviderHandler.class);
	private UnixSocketThread client;
	private UnixSocketResponsHandler handler;
	private byte[] yourQuoted;
	private byte[] yourSignature;
	private byte[] cert;
	private boolean signatureCorrect;
	private ConnectorMessage msg;
	private TrustedThirdParty ttp;
	private long sessionID = 0;
	private URI ttpUri;
	private boolean pcrCorrect;
	
	public RemoteAttestationProviderHandler(FSM fsm, IdsAttestationType type, URI ttpUri) {
		// set ttp uri
		this.ttpUri = ttpUri;
		// set finite state machine
		this.fsm = fsm;
		// set current attestation type (see attestation.proto)
		this.aType = type;
		// try to start new Thread:
		// UnixSocketThread will be used to communicate with local TPM2d		
		try {
			// client will be used to send messages
			this.client = new UnixSocketThread(SOCKET);
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
		// get starting session id
		this.sessionID = e.getMessage().getId();
		// construct protobuf message to send to local tpm2d via unix socket and try to talk to local unix socket
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(this.aType)
				.setQualifyingData(this.yourNonce)
				.setCode(Code.INTERNAL_ATTESTATION_REQ)
				.build();
		try {
			if(thread.isAlive()) {
				// send msg to local unix socket
				client.send(msg.toByteArray(), this.handler);
				byte[] toParse = this.handler.waitForResponse();
				TpmToController response = TpmToController.parseFrom(toParse);
				// now return values from answer to client
				return ConnectorMessage
						.newBuilder()
						.setId(++this.sessionID)
						.setType(ConnectorMessage.Type.RAT_RESPONSE)
						.setAttestationResponse(
								AttestationResponse
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.myNonce)
								.setHalg(response.getHalg())
								.setQuoted(response.getQuoted())
								.setSignature(response.getSignature())
								.addAllPcrValues(response.getPcrValuesList())
								.setCertificateUri(response.getCertificateUri())
								.build()
								)
						.build();				
			}
			else {
				String error = "error: RAT server thread is not alive !";
				LOG.debug(error);
				return RemoteAttestationHandler.sendError(this.thread, "thread error", error);	
			}
		} catch (IOException ex) {
			String error = "error: IOException :" + ex.getMessage();
			LOG.debug(error);
			ex.printStackTrace();
			return RemoteAttestationHandler.sendError(this.thread, ex.getStackTrace().toString(), error);	
		} catch (InterruptedException ex) {
			String error = "error: InterruptedException :" + ex.getMessage();
			LOG.debug(error);
			ex.printStackTrace();
			return RemoteAttestationHandler.sendError(this.thread, ex.getStackTrace().toString(), error);	
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
		this.cert = DatatypeConverter.parseHexBinary(e.getMessage().getAttestationResponse().getCertificateUri());
		// get pcr values from server msg
		int numPcrValues = e.getMessage().getAttestationResponse().getPcrValuesCount();
		PcrValue[] values = TrustedThirdParty.fill(e.getMessage().getAttestationResponse().getPcrValuesList().toArray(new Pcr[numPcrValues]));
		this.ttp = new TrustedThirdParty(ttpUri);
		this.pcrCorrect = this.ttp.pcrValuesCorrect(values, NonceGenerator.generate());
		if(this.sessionID + 1 == e.getMessage().getId()) {
			++this.sessionID;
			try {
				// construct a new TPM2B_PUBLIC from bkey bytes
				TPM2B_PUBLIC key = new TPM2B_PUBLIC(this.cert);
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
				String error = "error: Exception when talking to tpm2d :" + ex.getMessage();
				LOG.debug(error);
				ex.printStackTrace();
				return RemoteAttestationHandler.sendError(this.thread, ex.getStackTrace().toString(), error);	
			}
			return ConnectorMessage
					.newBuilder()
					.setId(++this.sessionID)
					.setType(ConnectorMessage.Type.RAT_RESULT)
					.setAttestationResult(
							AttestationResult
							.newBuilder()
							.setAtype(this.aType)
							.setResult(this.signatureCorrect && pcrCorrect)
							.build())
					.build();
		}
		else {
			String error = "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")";
			LOG.debug(error);
			return RemoteAttestationHandler.sendError(this.thread, "sessionID", error);
		}
	}

	public MessageLite leaveRatRequest(Event e) {
		this.thread.interrupt();
		if(e.getMessage().getId() == this.sessionID + 1) {
			++this.sessionID;
			return ConnectorMessage
					.newBuilder()
					.setId(++this.sessionID)
					.setType(ConnectorMessage.Type.RAT_LEAVE)
					.setAttestationLeave(
							AttestationLeave
							.newBuilder()
							.setAtype(this.aType)
							.build()
							)
					.build();
		}
		else {
			String error = "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")";
			LOG.debug(error);
			return RemoteAttestationHandler.sendError(this.thread, "sessionID", error);
		}

	}
}
