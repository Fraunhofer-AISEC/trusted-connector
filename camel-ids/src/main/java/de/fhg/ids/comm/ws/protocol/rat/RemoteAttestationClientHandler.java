package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.Pcr;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.ByteArrayUtil;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.NonceGenerator;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tools.PublicKeyConverter;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm.TPM_ALG_ID.ALG_ID;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b.TPM2B_DIGEST;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpm2b.TPM2B_PUBLIC;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpms.TPMS_ATTEST;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmt.TPMT_SIGNATURE;
import de.fhg.ids.comm.ws.protocol.rat.tpm20.tpmu.TPMU_ATTEST;


public class RemoteAttestationClientHandler {
	private final FSM fsm;
	private String SOCKET = "mock/tpm2dc.sock";
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationClientHandler.class);
	private UnixSocketResponsHandler handler;
	private UnixSocketThread client;
	private Thread thread;
	private byte[] yourQuoted;
	private byte[] yourSignature;
	private String certUri;
	private boolean signatureCorrect = false;
	private Pcr[] pcrValues;
	
	public RemoteAttestationClientHandler(FSM fsm, IdsAttestationType type) {
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
		// add bouncy castle security provider
		//Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	public MessageLite enterRatRequest(Event e) {
		// generate a new software nonce on the client
		this.myNonce = NonceGenerator.generate();
		// and send it to server
		// together with the current attestation type
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_REQUEST)
				.setAttestationRequest(
						AttestationRequest
						.newBuilder()
						.setAtype(this.aType)
						.setQualifyingData(this.myNonce)
						.build())
				.build();			
	}
	
	public MessageLite sendTPM2Ddata(Event e) {
		
		Signature sg;
		// get nonce from server msg
		this.yourNonce = e.getMessage().getAttestationResponse().getQualifyingData().toString();
		// get quote from server msg
		this.yourQuoted = DatatypeConverter.parseHexBinary(e.getMessage().getAttestationResponse().getQuoted());
		// get signature from server msg
		this.yourSignature = DatatypeConverter.parseHexBinary(e.getMessage().getAttestationResponse().getSignature());
		// get cert uri from server msg
		this.certUri = e.getMessage().getAttestationResponse().getCertificateUri();

		
		try {
			// construct a new TPM2B_PUBLIC from bkey bytes
			TPM2B_PUBLIC key = new TPM2B_PUBLIC(this.fetchPublicKey(this.certUri));
			// and convert it into an DER key
			PublicKey publicKey = new PublicKeyConverter(key).getPublicKey();
			// construct a new TPMT_SIGNATURE from yourSignature bytes
			TPMT_SIGNATURE tpmSignature = new TPMT_SIGNATURE(this.yourSignature);
			// and get the raw byte signature
			byte[] signature = tpmSignature.getSignature().getSig();
			// construct a new TPMS_ATTEST from yourQuoted bytes
			TPMS_ATTEST digest = new TPMS_ATTEST(this.yourQuoted);
			// and get the raw byte quote
			byte[] quote = digest.toBytes();
			switch(tpmSignature.getSignature().getHashAlg()) {
				case TPM_ALG_SHA256:
					sg = Signature.getInstance("SHA256withRSA");
				    sg.initVerify(publicKey);
				    sg.update(this.yourQuoted);
				    this.signatureCorrect = sg.verify(signature);
					break;
				case TPM_ALG_SHA1:
					sg = Signature.getInstance("SHA1withRSA");
				    sg.initVerify(publicKey);
				    sg.update(this.yourQuoted);
				    this.signatureCorrect = sg.verify(signature);
					break;					
				default:
					break;
			}
			
		} catch (Exception ex) {
			LOG.debug("error: could not fetch public key from \""+this.certUri+"\":" + ex.getMessage());
			ex.printStackTrace();
			return ControllerToTpm
					.newBuilder()
					.build();
		}
			
		if(thread.isAlive()) {
			// send msg to local unix socket
			try {
				// construct protobuf message to send to local tpm2d via unix socket
				ControllerToTpm msg = ControllerToTpm
						.newBuilder()
						.setAtype(this.aType)
						.setQualifyingData(this.yourNonce)
						.setCode(Code.INTERNAL_ATTESTATION_REQ)
						.build();
				client.send(msg.toByteArray(), this.handler);
				// and wait for response
				TpmToController answer = this.handler.waitForResponse();
				// now return values from answer to server
				return ConnectorMessage
						.newBuilder()
						.setId(0)
						.setType(ConnectorMessage.Type.RAT_RESPONSE)
						.setAttestationResponse(
								AttestationResponse
								.newBuilder()
								.setAtype(this.aType)
								.setHalg(answer.getHalg())
								.setQuoted(answer.getQuoted())
								.setSignature(answer.getSignature())
								.addAllPcrValues(answer.getPcrValuesList())
								.setCertificateUri(answer.getCertificateUri())
								.build()
								)
						.build();
			} catch (IOException e1) {
				LOG.debug("error: IOException");
				e1.printStackTrace();
				return null;
			} catch (InterruptedException e1) {
				LOG.debug("error: InterruptedException");
				e1.printStackTrace();
				return null;
			}
		}
		else {
			LOG.debug("error: thread is not alive");
			return null;
		}
	}

	public MessageLite sendResult(Event e) {
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_RESULT)
				.setAttestationResult(
						AttestationResult
						.newBuilder()
						.setAtype(this.aType)
						.setResult(this.isAttestationSuccessful())
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

	private boolean isAttestationSuccessful() {
		return this.signatureCorrect && TrustedThirdParty.pcrValuesCorrect(this.pcrValues);
	}
	
	// fetch a public key from a uri and return the key as a byte array
	private byte[] fetchPublicKey(String uri) throws Exception {
		URL cert = new URL(uri);
		BufferedReader in = new BufferedReader(new InputStreamReader(cert.openStream()));
		String base64 = "";
		String inputLine = "";
        while ((inputLine = in.readLine()) != null) {
        	base64 += inputLine;
        }
        in.close();
        return javax.xml.bind.DatatypeConverter.parseBase64Binary(base64);
	}
}
